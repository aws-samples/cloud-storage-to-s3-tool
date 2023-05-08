package org.aws.gcrrp.datatos3.controller;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import net.sf.json.JSONObject;
import org.aws.gcrrp.datatos3.model.EC2Instance;
import org.aws.gcrrp.datatos3.repository.DynamoDBRepository;
import org.aws.gcrrp.datatos3.repository.SQSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.aws.gcrrp.datatos3.model.CloneConfig;
import org.aws.gcrrp.datatos3.model.CloneTask;
import org.aws.gcrrp.datatos3.model.ConfigInfo;
import org.aws.gcrrp.datatos3.utils.ENVConfig;

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class TaskController {
    Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private DynamoDBRepository dynamoDBRepository;

    @Autowired
    private SQSRepository sqsRepository;

    @Value("${dynamodb.table.name.instance}")
    private String instanceTableName;

    @Value("${dynamodb.table.name.task}")
    private String taskTableName;

    @Value("${dynamodb.table.name.config}")
    private String configTableName;

    @Value("${sqs.queue.name}")
    private String queueName;

    @Autowired
    private APIController aPIController;

    static Map<String, String> configMap = Collections.synchronizedMap(new HashMap<>());

    SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/getCloneInfo")
    public Map<String, String> getCloneInfo(@RequestParam("s3Path") String s3Path,
                                            @RequestParam("gcsPath") String gcsPath,
                                            @RequestParam(required = false, name = "file") MultipartFile file,
                                            @RequestParam("amiId") String amiId,
                                            @RequestParam("awsCloneLogBucket") String awsCloneLogBucket,
                                            @RequestParam("rcloneCommand") String rcloneCommand,
                                            @RequestParam("emailTo") String emailTo
    ) throws Exception {

        ENVConfig.amiID = amiId;
        ENVConfig.rcloneCommand = rcloneCommand;
        ENVConfig.transferS3LogPath = awsCloneLogBucket;
        ENVConfig.emailFrom = emailTo;
        ENVConfig.emailTo = emailTo;

        configMap.put("ami_id", amiId);
        configMap.put("rclone_command", rcloneCommand);
        configMap.put("aws_clone_log_bucket", awsCloneLogBucket);
        configMap.put("email_from", emailTo);
        configMap.put("email_to", emailTo);
        configMap.put("gcs_path", gcsPath);
        configMap.put("aws_s3_path", s3Path);

        // Write config, if exist update it , else create it.
        List<CloneConfig> configList = dynamoDBRepository.queryConfigByKey("clone_configuration");
        if (configList.size() > 0) {
            CloneConfig config = configList.get(0);
            config.getInfo().setAmiID(amiId);
            config.getInfo().setAwsRegion(ENVConfig.awsRegion);
            config.getInfo().setRcloneCommand(rcloneCommand);
            config.getInfo().setTransferS3LogBucket(awsCloneLogBucket);
            config.getInfo().setEmailTo(emailTo);
            config.getInfo().setEmailFrom(emailTo);
            config.getInfo().setSqsQueueUrl(ENVConfig.sqsQueueUrl);
            config.getInfo().setSourceBucketName(gcsPath);
            config.getInfo().setDestinationBucketName(s3Path);
            dynamoDBRepository.updateConfigItem(config);
        } else {
            ConfigInfo configInfo = new ConfigInfo(ENVConfig.awsRegion, amiId, awsCloneLogBucket, rcloneCommand, emailTo, emailTo, ENVConfig.sqsQueueUrl, gcsPath, s3Path);
            CloneConfig cloneConfig = new CloneConfig("clone_configuration", configInfo);
            dynamoDBRepository.createConfigItem(cloneConfig);
        }

        if (file != null) {
            CsvReader reader = CsvUtil.getReader();
            //从文件中读取CSV数据
            CsvData data = reader.read(new InputStreamReader(file.getInputStream()));
            List<CsvRow> rows = data.getRows();
            List<String> inputPathList = new ArrayList<String>();
            rows.stream().forEach(line -> {
                inputPathList.add(line.getRawList().get(0));
                String dateTime = FORMAT.format(new java.util.Date());
                CloneTask task = new CloneTask(line.getRawList().get(0), gcsPath, s3Path, dateTime);
                dynamoDBRepository.createTaskItem(task);

                String messageBody = JSONObject.fromObject(task).toString();
                sqsRepository.sendMsgToSQSQueue(messageBody);

            });

            // Set send message after data clone complete.
            logger.info("传送完成邮件开关置为开，当传输完成后会发送邮件通知。");
            ENVConfig.sendMessage = true;
        }


        return new HashMap<String, String>() {{
            put("result", "OK");
        }};
    }

    @RequestMapping(value = "/getCloneConfig")
    public JSONObject getCloneConfig() {
        logger.debug("invoke getCloneConfig");
        return JSONObject.fromObject(configMap);
    }

    @RequestMapping(value = "/getTaskList")
    public JSONObject getInstanceOn() {
        logger.debug("invoke getInstanceOn");
        List<CloneTask> taskList = dynamoDBRepository.scanALLTaskItems();
        Map<String, CloneTask> taskMap = new HashMap<String, CloneTask>();
        for (CloneTask task : taskList) {
            taskMap.put(task.getClonePath(), task);
        }
        return JSONObject.fromObject(taskMap);
    }

    @PostConstruct
    public void initDDBTables() {
        //aws_region
        String awsRegion = System.getProperty("aws_region");
        ENVConfig.awsRegion = awsRegion;

        // init SQS queue
        logger.info("Init SQS queue");
        String queueUrl = sqsRepository.createQueue(queueName);
        ENVConfig.sqsQueueUrl = queueUrl;

        //init instanceTableName and taskTableName
        logger.info("Init createTable instanceTableName and taskTableName");
        dynamoDBRepository.createTable(instanceTableName, "instanceId");
        dynamoDBRepository.createTable(taskTableName, "clonePath");
        dynamoDBRepository.createTable(configTableName, "configKey");

        // init instanceTableName and taskTableName
        logger.info("add EC2s from DDB to memory");
        for (EC2Instance eC2Instance : dynamoDBRepository.scanALLInstances()) {
            // dynamoDBRepository.deleteInstanceItem(eC2Instance);
            aPIController.allEC2List.add(eC2Instance.getInstanceId());
            if (eC2Instance.getInfo().getInstanceType().equals("Undone")) {
                aPIController.unInstallList.add(eC2Instance.getInstanceId());
            } else {
                aPIController.installedToolsList.add(eC2Instance.getInstanceId());
            }
            List<EC2Instance> listEC2Instance = dynamoDBRepository.queryInstanceById(eC2Instance.getInstanceId());
            if (listEC2Instance.size() != 0) {

                EC2Instance EC2Instance = listEC2Instance.get(0);
                List<CloneTask> cloneTasksList = dynamoDBRepository.queryTaskByPath(EC2Instance.getInfo().getClonePath());
                if (cloneTasksList.size() != 0) {
                    CloneTask cloneTask = cloneTasksList.get(0);
                    logger.info(eC2Instance.getInstanceId() + "正在执行的任务：" + EC2Instance.getInfo().getClonePath());
                    aPIController.map_cloneTask.put(eC2Instance.getInstanceId(), cloneTask);
                }
            }
            aPIController.map_runningInstance.put(eC2Instance.getInstanceId(), eC2Instance);
        }


        //读DDB 放到EVNconfig.email = ""
        List<CloneConfig> configList = dynamoDBRepository.queryConfigByKey("clone_configuration");
        if (configList.size() > 0) {
            CloneConfig config = configList.get(0);
            ENVConfig.amiID = config.getInfo().getAmiID();
            ENVConfig.awsRegion = config.getInfo().getAwsRegion();
            ENVConfig.rcloneCommand = config.getInfo().getRcloneCommand();
            ENVConfig.transferS3LogPath = config.getInfo().getTransferS3LogBucket();
            ENVConfig.emailFrom = config.getInfo().getEmailFrom();
            ENVConfig.emailTo = config.getInfo().getEmailTo();
            ENVConfig.sqsQueueUrl = config.getInfo().getSqsQueueUrl();

            configMap.put("ami_id", config.getInfo().getAmiID());
            configMap.put("aws_region", config.getInfo().getAwsRegion());
            configMap.put("rclone_command", config.getInfo().getRcloneCommand());
            configMap.put("aws_clone_log_bucket", config.getInfo().getTransferS3LogBucket());
            configMap.put("email_from", config.getInfo().getEmailFrom());
            configMap.put("email_to", config.getInfo().getEmailTo());
            configMap.put("sqs_queue_url", config.getInfo().getSqsQueueUrl());
            configMap.put("gcs_path", config.getInfo().getSourceBucketName());
            configMap.put("aws_s3_path", config.getInfo().getDestinationBucketName());
        }
    }
}
