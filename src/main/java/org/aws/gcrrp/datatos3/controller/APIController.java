package org.aws.gcrrp.datatos3.controller;

import net.sf.json.JSONObject;
import org.aws.gcrrp.datatos3.model.CloneTask;
import org.aws.gcrrp.datatos3.model.EC2Instance;
import org.aws.gcrrp.datatos3.model.InstanceInfo;
import org.aws.gcrrp.datatos3.repository.DynamoDBRepository;
import org.aws.gcrrp.datatos3.repository.EmailHandler;
import org.aws.gcrrp.datatos3.repository.SQSRepository;
import org.aws.gcrrp.datatos3.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.CommandInvocation;
import software.amazon.awssdk.services.ssm.model.ListCommandInvocationsRequest;
import software.amazon.awssdk.services.ssm.model.SendCommandRequest;
import software.amazon.awssdk.services.ssm.model.SendCommandResponse;


import java.util.*;

@RestController
public class APIController {

    static Logger logger = LoggerFactory.getLogger(APIController.class);

    static Map<String, EC2Instance> map_runningInstance = Collections.synchronizedMap(new HashMap<>());

    static Map<String, CloneTask> map_cloneTask = Collections.synchronizedMap(new HashMap<>());

    //所有 EC2集群
    static ArrayList<String> allEC2List = new ArrayList<>();
    //未安装插件
    static ArrayList<String> unInstallList = new ArrayList<>();
    //安装插件后的EC2集群
    static ArrayList<String> installedToolsList = new ArrayList<>();

    //    @Autowired
    private DynamoDBRepository dynamoDBRepository;


    @Autowired
    private SQSRepository sqsRepository;

    public static APIController aPIController;


    @RequestMapping(value = "/createCluster", method = RequestMethod.POST)
    public void createCluster(@RequestBody JSONObject jsonObject) {
        logger.info("invoke createCluster");
        Date date = new Date();
        String num = jsonObject.get("num").toString();
        String type = jsonObject.get("type").toString();
        logger.info(num);
        logger.info(type);
        String name = "rclone-instance";
        String amiId = ENVConfig.amiID;
        this.createEC2Instance(name, amiId, Integer.parseInt(num), type);
    }


    @GetMapping(value = "/terminateCluster")
    public void terminateCluster() {
        logger.info("invoke terminateCluster");
        this.terminateEC2(allEC2List);
    }

    @GetMapping(value = "/ping")
    public String isHealthy() {
        logger.debug("invoke isHealthy");
        return "pang";
    }


    @RequestMapping(value = "/getInstanceOn")
    public JSONObject getInstanceOn() {
        logger.debug("invoke getInstanceOn");
        return JSONObject.fromObject(map_runningInstance);
    }


    public boolean createEC2Instance(String name, String amiId, int num, String type) {
        Ec2Client ec2 = EC2ClientSingleton.getInstance();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.fromValue(type))
                .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("Rclone-EC2-Instance-Role").build())
                .maxCount(num)
                .minCount(num)
                .build();

        RunInstancesResponse response = ec2.runInstances(runRequest);
        //Todo 增加tag
        Tag tag = Tag.builder().key("Name").value(name).build();

        for (Instance instance : response.instances()) {
            String instanceId = instance.instanceId();
            CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                    .resources(instanceId)
                    .tags(tag)
                    .build();
            try {
                ec2.createTags(tagRequest);
                unInstallList.add(instanceId);
                allEC2List.add(instanceId);

                //添加到DDB
                EC2Instance eC2Instance = new EC2Instance(instanceId, "Undone", "", instance.launchTime().toString());
                map_runningInstance.put(instanceId, eC2Instance);
                this.dynamoDBRepository = DDBClientSingleton.getInstance();
                dynamoDBRepository.createInstanceItem(eC2Instance);
                logger.info("Successfully started EC2 Instance [{}] based on AMI [{}]", instanceId, amiId);
            } catch (Ec2Exception e) {
                logger.error(e.awsErrorDetails().errorMessage());
                return false;
            }
        }
        return true;
    }

    // 删除所有正在运行的EC2
    public void terminateEC2(List<String> runningList) {
        Ec2Client ec2 = EC2ClientSingleton.getInstance();
        String[] instanceIDs = runningList.toArray(new String[runningList.size()]);
        for (String instance : instanceIDs) {

            allEC2List.remove(instance);

            unInstallList.remove(instance);

            installedToolsList.remove(instance);

            map_runningInstance.remove(instance);

            //DDB中删除
            this.dynamoDBRepository = DDBClientSingleton.getInstance();
            dynamoDBRepository.deleteInstanceItem(instance);

        }
        logger.info("invoke terminateCluster instanceIDs:" + instanceIDs.toString());
        try {
            TerminateInstancesRequest terminateInstancesRequest = TerminateInstancesRequest.builder()
                    .instanceIds(instanceIDs)
                    .build();

            TerminateInstancesResponse response = ec2.terminateInstances(terminateInstancesRequest);
            List<InstanceStateChange> instanceStateChangelist = response.terminatingInstances();

            for (InstanceStateChange instanceStateChange : instanceStateChangelist) {
                logger.info("The ID of the terminated instance is " + instanceStateChange.instanceId());
            }
            runningList.clear();
        } catch (Ec2Exception e) {
            logger.error(e.awsErrorDetails().errorMessage());
        }
    }

    public void checkSystemStatusOK(Ec2Client ec2Client, String... instanceIDs) {
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            DescribeInstanceStatusResponse describeInstanceStatusResponse = ec2Client.describeInstanceStatus(DescribeInstanceStatusRequest.builder().instanceIds(instanceIDs).build());
            List<InstanceStatus> statusList = describeInstanceStatusResponse.instanceStatuses();
            boolean isOK = true;
            for (InstanceStatus status : statusList) {
                if (status.systemStatus().status().equals(SummaryStatus.INITIALIZING)) {
                    logger.info("SystemStatus is INITIALIZING");
                    isOK = false;
                } else {
                    isOK = true;
                }
            }
            if (isOK) {
                logger.info("SystemStatus is OK");
                break;
            }
        }
    }

    public ArrayList<String> installCommandList(String config, ArrayList<String> commandList) {
        String ssmCommand = "curl https://rclone.org/install.sh | sudo bash";
        commandList.add(ssmCommand);
        ssmCommand = "sudo mkdir -p /home/ssm-user/.config/rclone/";
        commandList.add(ssmCommand);
        ssmCommand = "sudo mkdir -p /root/.config/rclone/";
        commandList.add(ssmCommand);
        ssmCommand = "cat << EOF > /home/ssm-user/.config/rclone/rclone.conf\n" +
                config + "\n" +
                "EOF";
        commandList.add(ssmCommand);
        ssmCommand = "cat << EOF > /root/.config/rclone/rclone.conf\n" +
                config + "\n" +
                "EOF";
        commandList.add(ssmCommand);
        return commandList;
    }

    @RequestMapping(value = "/installTools", method = RequestMethod.POST)
    public void installTools(@RequestBody JSONObject jsonObject) {
        logger.info("check unInstallList SystemStatus");
        Ec2Client ec2 = EC2ClientSingleton.getInstance();
        if (unInstallList.size() != 0) {
            String[] instanceID = this.getUnInstallList();
            //检查所有的ec2的状态是否已经初始化完成。
            this.checkSystemStatusOK(ec2, instanceID);
            logger.info("start installTools");

            String config = jsonObject.get("config").toString();
            ArrayList<String> commandList = new ArrayList<String>();
            //获得安装插件的命令列表
            commandList = this.installCommandList(config, commandList);
            this.runCommand(commandList, false, instanceID);
            commandList.clear();
            commandList.add("rclone config show");
            while (true) {
                try {
                    Thread.sleep(20000);
                    List<CommandInvocation> invocationList = this.runCommand(commandList, true, instanceID);
                    boolean isComplete = true;
                    for (CommandInvocation invocation : invocationList) {
                        String commandOutput = invocation.commandPlugins().get(0).output();
                        if (commandOutput.contains("type = s3")) {
                            //插件安装完毕
                            logger.info("插件安装完毕:" + invocation.instanceId());
                            if (!installedToolsList.contains(invocation.instanceId())) {
                                installedToolsList.add(invocation.instanceId());
                                unInstallList.remove(invocation.instanceId());
                                //添加到DDB
                                EC2Instance eC2Instance = map_runningInstance.get(invocation.instanceId());
                                InstanceInfo instanceInfo = eC2Instance.getInfo();
                                instanceInfo.setInstanceType("Done");
                                eC2Instance.setInfo(instanceInfo);
                                dynamoDBRepository = DDBClientSingleton.getInstance();
                                dynamoDBRepository.updateInstanceItem(eC2Instance);
                                logger.debug("update DDB updateInstanceItem");
                            }
                        } else {
                            isComplete = false;
                            logger.info("插件没有安装完毕" + invocation.instanceId());
                        }
                    }
                    if (isComplete) {
                        logger.info("所有插件都安装完毕");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //检查现有集群内机器是否完成rclone的传输任务
    public boolean checkComplete() {
        ENVConfig.isCompleted = true;
        String[] instancesArr = this.getInstalledToolsList();
        if (instancesArr.length != 0) {
            logger.info("invoke checkComplete,检查所有的已安装插件的机器的传输任务状态");
            ArrayList<String> commandList = new ArrayList<String>();
            String ssmCommand = "ps -ef|grep rclone";
            commandList.add(ssmCommand);
            List<CommandInvocation> invocationList = this.runCommand(commandList, true, instancesArr);
            for (CommandInvocation invocation : invocationList) {
                String commandOutput = invocation.commandPlugins().get(0).output();
                String status = invocation.commandPlugins().get(0).statusAsString();
//                logger.info(status);
                //判断是否包含 ps 中是否包含 rclone copy -v，存在即还有任务在传输
                //if not
                if (!commandOutput.contains("rclone copy -v") && status.equals("Success")) {
                    dynamoDBRepository = DDBClientSingleton.getInstance();
                    String instanceId = invocation.instanceId();
                    if (map_runningInstance.containsKey(instanceId)) {
                        ArrayList<String> md5CommandList = new ArrayList<String>();
                        EC2Instance ec2Instance = map_runningInstance.get(instanceId);
                        CloneTask cloneTask = map_cloneTask.get(instanceId);
//                        map_runningInstance.put(instanceId,map_runningInstance.get(instanceId).getInfo().setClonePath(""));
                        ;
                        if (cloneTask != null) {
                            String cloneLogPath = getLogPath(cloneTask.getClonePath());
//                        Config.md5_check;
                            //读取双向 md 日志
                            ssmCommand = "rclone md5sum " + cloneTask.getInfo().getSourceLocation() + "> \"/usr/tmp/" + cloneLogPath + "/source-md.log\" 2>&1";
                            md5CommandList.add(ssmCommand);
                            ssmCommand = "rclone md5sum " + cloneTask.getInfo().getDestinationLocation() + "> \"/usr/tmp/" + cloneLogPath + "/destination-md.log\" 2>&1";
                            md5CommandList.add(ssmCommand);
                            ssmCommand = "tail -n 5 /usr/tmp/" + cloneLogPath + "/rclone.log";
                            md5CommandList.add(ssmCommand);
                            ssmCommand = "mkdir -p /usr/tmp/log/" + cloneLogPath + "/rclone.log";
                            md5CommandList.add(ssmCommand);
                            ssmCommand = "rclone copy -v /usr/tmp/" + cloneLogPath + " " + ENVConfig.transferS3LogPath + cloneLogPath + "> \"/usr/tmp/log/" + cloneLogPath + ".log\" 2>&1";
                            md5CommandList.add(ssmCommand);
                            List<CommandInvocation> checkInvocationList = this.runCommand(md5CommandList, true, instanceId);
                            //TODO get
//                            Transferred:   	    1.423 GiB / 1.423 GiB, 100%, 97.149 MiB/s, ETA 0s
//                            Transferred:            5 / 5, 100%
//                                    Elapsed time:        15.2s
                            String output = checkInvocationList.get(0).commandPlugins().get(0).output();
                            if (!output.contains("100%")) {
                                //不成功，抓到任务重新放到传输队列
                                map_cloneTask.get(instanceId);
                                logger.error("Task transfer failed and the task resent to the task queue.");
                                String messageBody = JSONObject.fromObject(cloneTask).toString();
                                cloneTask.getInfo().setTaskStatus("rerun");
                                new SQSRepository().sendMsgToSQSQueue(messageBody);
                                dynamoDBRepository.updateTaskItem(cloneTask);
                                new EmailHandler().sendEmail("The transmission mission failed" + cloneTask.getClonePath(), "The task has been put back into the task queue.");
                            } else {
                                cloneTask.getInfo().setTransferSizeAndRate(getTransferResultMetrics(output, "ETA"));
                                cloneTask.getInfo().setTransferFileCount(getTransferResultMetrics(output, "Transferred"));
                                cloneTask.getInfo().setTransferElapsedTime(getTransferResultMetrics(output, "Elapsed time"));
                                cloneTask.getInfo().setTaskStatus("done");
                                cloneTask.getInfo().setUpdateTime(new Date().toString());
                                cloneTask.getInfo().setS3LogUri(ENVConfig.transferS3LogPath + cloneLogPath);
                                dynamoDBRepository.updateTaskItem(cloneTask);
                                logger.info("Task" + cloneTask.getClonePath() + " transfer success.");
                            }
                        }
                        logger.debug("建立新的传输任务");
                        //取消息
//                        map_runningInstance.remove()
                        sqsRepository = SQSClientSingleton.getInstance();
                        cloneTask = sqsRepository.receiveMessageFromSQSQueue();
                        if (cloneTask == null) {

                            //删除正在跑的task
                            map_cloneTask.remove(instanceId);
                            //ec2Instance将在跑的task置为空
                            ec2Instance.getInfo().setClonePath("");
                            map_runningInstance.put(instanceId, ec2Instance);
                            dynamoDBRepository.updateInstanceItem(ec2Instance);
                            logger.info("任务队列为空，已经处理完毕:" + ec2Instance.getInstanceId());
//                          this.terminateCluster();
//                            continue;
                        } else {
                            String logPath = this.getLogPath(cloneTask.getClonePath());
                            //下发任务TaskCommandList
                            ArrayList<String> addTaskCommandList = new ArrayList<>();

                            String logMakeDirCommand = "mkdir -p /usr/tmp/" + logPath;
                            addTaskCommandList.add(logMakeDirCommand);
                            ENVConfig.rcloneCommand.replace("", "");
                            String addTaskCommand = ENVConfig.rcloneCommand.replace("{SourceLocation}", cloneTask.getInfo().getSourceLocation()).replace("{DestinationLocation}", cloneTask.getInfo().getDestinationLocation()) + " > \"/usr/tmp/" + logPath + "/rclone.log\" 2>&1 &";
                            addTaskCommandList.add(addTaskCommand);

                            this.runCommand(addTaskCommandList, false, instanceId);
                            logger.info("rclone addTaskCommand" + addTaskCommand);

                            //map_cloneTask 任务添加正在执行的任务
                            cloneTask.getInfo().setTaskStatus("running");
                            cloneTask.getInfo().setInstanceId(instanceId);
                            cloneTask.getInfo().setStartCloneTime(new Date().toString());
                            cloneTask.getInfo().setUpdateTime(new Date().toString());
                            map_cloneTask.put(instanceId, cloneTask);
                            ec2Instance.getInfo().setClonePath(cloneTask.getClonePath());

                            //更新map_runningInstance 中正在执行的ClonePath
                            map_runningInstance.put(instanceId, ec2Instance);

                            //DDB中更新instance信息
                            dynamoDBRepository.updateInstanceItem(ec2Instance);

                            //DDB中更新clone task 状态，根据客户实际需要，有的消息是SQS来的。
                            dynamoDBRepository.createTaskItem(cloneTask);

                            //新任务添加
                            ENVConfig.isCompleted = false;
                        }
                    }
                } else {
                    //任务没执行完毕
                    ENVConfig.isCompleted = false;
                    logger.info(invocation.instanceId() + "正在传输任务");
                }
            }
            if (ENVConfig.isCompleted && ENVConfig.sendMessage) {
                new EmailHandler().sendEmail("All tasks have been completed", "All tasks have been completed！Please shut down all transmission machines！");
                ENVConfig.sendMessage = false;
                logger.info("sendMessage开关置为false，不再继续发送邮件。");
            }
        }
        return true;
    }

    public List<CommandInvocation> runCommand(ArrayList<String> ssmCommandList, boolean needLogBack, String... instanceIDs) {
        Map<String, List<String>> params = new HashMap<String, List<String>>();

        params.put("commands", ssmCommandList);
        SendCommandRequest commandRequest = SendCommandRequest.builder()
                .documentName("AWS-RunShellScript")
                .instanceIds(instanceIDs)
                .parameters(params)
                .build();
        SsmClient ssmClient = SsmClientSingleton.getInstance();
        SendCommandResponse commandResult = ssmClient.sendCommand(commandRequest);
        String id = commandResult.command().commandId();
        if (needLogBack) {
            ListCommandInvocationsRequest request;
            List<CommandInvocation> invocationList;
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                request = ListCommandInvocationsRequest.builder().commandId(id).details(true).build();
                invocationList = ssmClient.listCommandInvocations(request).commandInvocations();
                if (invocationList.get(0).commandPlugins().get(0).status().toString().equals("Success")) {
                    if (invocationList.get(0).status().toString().equals("Success"))
//                        logger.info("Success");
//                        logger.info(invocationList.get(0).status()+"3");
                        break;
                } else if (invocationList.get(0).commandPlugins().get(0).status().toString().equals("Failed")) {
//                    logger.info("Failed");
//                    this.runCommand(ssmCommandList,needLogBack,instanceIDs);
                    break;
                } else if (invocationList.get(0).status().toString().equals("Failed")) {
                    break;
                }
            }
            return invocationList;
        }
        return null;
    }


    public void printList() {
        logger.info("传输机器有:" + allEC2List.toString() + ",未安装插件的机器有:" + unInstallList.toString() + ",安装插件的机器有:" + installedToolsList.toString());
    }

    public String[] getUnInstallList() {
        return unInstallList.toArray(new String[unInstallList.size()]);
    }

    public String[] getInstalledToolsList() {
        return installedToolsList.toArray(new String[installedToolsList.size()]);
    }

    public String getLogPath(String path) {
        if (path != null && path.contains("/")) {
            return path.replaceAll("/", "_").replaceAll("=", "_");
//            return path.substring(path.lastIndexOf("/")-10,path.lastIndexOf("/"));
        }
        return "ALL-LOG";
    }

    /**
     * 获取日志中传输结果的各个metric
     * @param resultLines
     * @param metricName
     * @return
     */
    private String getTransferResultMetrics(String resultLines, String metricName) {
        resultLines = resultLines.trim();
        String[] lines = resultLines.split(System.lineSeparator());
        for (String line : lines) {
            if (metricName.equals("Transferred") && line.contains("ETA")) {
                continue;
            }
            if (line.contains(metricName) && line.contains(":")) {
                String[] values = line.split("\t");
                if (values.length < 2) {
                    line = line.replace(" ", "");
                    values = line.split(":"); // 每行日志的格式不同, 分隔符也不同
                    if (values.length < 2) {
                        return "N/A";
                    } else {
                        return values[1].trim();
                    }
                } else {
                    return values[1].trim();
                }
            } else {
                continue;
            }
        }
        return "N/A";
    }
}

