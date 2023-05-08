package org.aws.gcrrp.datatos3.repository;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.gson.Gson;
import org.aws.gcrrp.datatos3.model.EC2Instance;
import org.aws.gcrrp.datatos3.utils.ENVConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.aws.gcrrp.datatos3.model.CloneConfig;
import org.aws.gcrrp.datatos3.model.CloneTask;
import org.aws.gcrrp.datatos3.utils.DynamoDBSingleton;
import net.sf.json.JSONObject;


import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The dynamodb APIs.
 *
 * @author JiaTing
 * @version 1.0
 * @since 2022-03-13
 */

@Component
public class DynamoDBRepository {

    static Logger logger = LoggerFactory.getLogger(DynamoDBRepository.class);

    @Autowired
    private DynamoDB dynamoDB;

    private String instanceTableName = "clone_instance";

    private String taskTableName = "clone_task";

    private String configTableName = "clone_config";


    SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Create item for clone task.
     *
     * @param task
     * @return
     */
    public String createTaskItem(CloneTask task) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(taskTableName);

        final Map<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("instanceId", task.getInfo().getInstanceId());
        infoMap.put("sourceLocation", task.getInfo().getSourceLocation());
        infoMap.put("destinationLocation", task.getInfo().getDestinationLocation());
        infoMap.put("insertTime", task.getInfo().getInsertTime());
        infoMap.put("updateTime", task.getInfo().getUpdateTime());
        infoMap.put("startCloneTime", task.getInfo().getStartCloneTime());
        infoMap.put("taskStatus", task.getInfo().getTaskStatus());
        infoMap.put("s3LogUri", task.getInfo().getS3LogUri());
        infoMap.put("transferSizeAndRate", "N/A");
        infoMap.put("transferFileCount", "N/A");
        infoMap.put("transferElapsedTime", "N/A");
        String message;
        try {
            logger.debug("Adding a new task item...");
            PutItemOutcome outcome = table
                    .putItem(new Item().withPrimaryKey("clonePath", task.getClonePath()).withMap("info", infoMap));
            message = "PutItem succeeded:\n" + outcome.getPutItemResult();
            logger.debug(message);

        } catch (Exception e) {
            message = "Unable to add item: " + task.getClonePath();
            logger.error(message);
            logger.error(e.getMessage());
        }

        return message;
    }

    /**
     * Scan all instance items in DynamoDB.
     *
     * @return
     */
    public List<CloneTask> scanALLTaskItems() {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        List<CloneTask> list = new ArrayList<CloneTask>();
        Table table = dynamoDB.getTable(taskTableName);
        ScanSpec scanSpec = new ScanSpec();

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            Iterator<Item> iter = items.iterator();
            Gson gson = new Gson();
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.debug(item.toJSONPretty());
                CloneTask task = gson.fromJson(item.toJSONPretty(), CloneTask.class);
                list.add(task);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table:");
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Query task items in DynamoDB by clone path.
     *
     * @return
     */
    public List<CloneTask> queryTaskByPath(String clonePath) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        List<CloneTask> list = new ArrayList<CloneTask>();
        Table table = dynamoDB.getTable(taskTableName);

        HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#ii", "clonePath");

        HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":yyyy", clonePath);

        QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#ii = :yyyy").withNameMap(nameMap)
                .withValueMap(valueMap);


        try {
            ItemCollection<QueryOutcome> items = table.query(querySpec);
            Iterator<Item> iter = items.iterator();
            Gson gson = new Gson();
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.debug(item.toJSONPretty());
                CloneTask task = gson.fromJson(item.toJSONPretty(), CloneTask.class);
                list.add(task);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table:");
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Update info for an instance by instance id.
     *
     * @param instance
     * @return
     */
    public String updateInstanceItem(EC2Instance instance) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(instanceTableName);
        String message;
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("instanceId", instance.getInstanceId())
                .withUpdateExpression("set info.startTime = :s, info.clonePath=:j, info.instanceType = :t")
                .withValueMap(new ValueMap().withString(":s", instance.getInfo().getStartTime())
                        .withString(":j", instance.getInfo().getClonePath())
                        .withString(":t", instance.getInfo().getInstanceType()))
                .withReturnValues(ReturnValue.UPDATED_NEW);

        try {
            logger.debug("Updating the item...");
            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            message = "UpdateItem succeeded:\n" + outcome.getItem().toJSONPretty();
            logger.debug(message);
        } catch (Exception e) {
            message = "Unable to add item: " + instance.getInstanceId();
            logger.error(message);
            logger.error(e.getMessage());
        }

        return message;
    }

    /**
     * Update info for a task by clone path.
     *
     * @param task
     * @return
     */
    public String updateTaskItem(CloneTask task) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(taskTableName);
        String message;

        String dateTime = FORMAT.format(new java.util.Date());
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("clonePath", task.getClonePath())
                .withUpdateExpression("set info.instanceId = :i, " +
                        "info.sourceLocation = :s, " +
                        "info.destinationLocation = :d, " +
                        "info.insertTime = :e, " +
                        "info.updateTime = :u, " +
                        "info.startCloneTime = :a, " +
                        "info.taskStatus = :x, " +
                        "info.s3LogUri = :r, " +
                        "info.transferSizeAndRate = :transferSizeAndRate, " +
                        "info.transferFileCount = :transferFileCount, " +
                        "info.transferElapsedTime = :transferElapsedTime")
                .withValueMap(new ValueMap().withString(":i", task.getInfo().getInstanceId())
                        .withString(":s", task.getInfo().getSourceLocation())
                        .withString(":d", task.getInfo().getDestinationLocation())
                        .withString(":e", task.getInfo().getInsertTime())
                        .withString(":u", dateTime)
                        .withString(":a", task.getInfo().getStartCloneTime())
                        .withString(":x", task.getInfo().getTaskStatus())
                        .withString(":r", task.getInfo().getS3LogUri())
                        .withString(":transferSizeAndRate", task.getInfo().getTransferSizeAndRate())
                        .withString(":transferFileCount", task.getInfo().getTransferFileCount())
                        .withString(":transferElapsedTime", task.getInfo().getTransferElapsedTime()))
                .withReturnValues(ReturnValue.UPDATED_NEW);

        try {
            logger.debug("Updating the item...");
            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            message = "UpdateItem succeeded:\n" + outcome.getItem().toJSONPretty();
            logger.debug(message);
        } catch (Exception e) {
            message = "Unable to update item: " + task.getClonePath();
            logger.error(message);
            logger.error(e.getMessage());
        }

        return message;
    }

    /**
     * Delete task item by clone path.
     *
     * @param task
     * @return
     */
    public String deleteTaskItem(CloneTask task) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(taskTableName);
        String message = "";
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("getClonePath", task.getClonePath()));
        // .withConditionExpression("info.rating <= :val")
//                .withValueMap(new ValueMap().withNumber(":val", 5.0));

        try {
            logger.debug("Attempting a conditional delete...");
            table.deleteItem(deleteItemSpec);
            message = "DeleteItem succeeded";
            logger.debug(message);
        } catch (Exception e) {
            message = "Unable to add item: " + task.getClonePath();
            logger.error(message);
            logger.error(e.getMessage());
        }
        return message;
    }


    // --- instance --

    /**
     * Scan all instance items in DynamoDB.
     *
     * @return
     */
    public List<EC2Instance> scanALLInstances() {
        logger.debug("scanALLInstances " + taskTableName);
        List<EC2Instance> list = new ArrayList<EC2Instance>();
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(instanceTableName);
        ScanSpec scanSpec = new ScanSpec();
        // .withProjectionExpression("#yr, title, info.rating");

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            Iterator<Item> iter = items.iterator();
            Gson gson = new Gson();
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.debug(item.toJSONPretty());
                EC2Instance instance = gson.fromJson(item.toJSONPretty(), EC2Instance.class);
                list.add(instance);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table:instanceTable");
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Scan  instance item by clone path in DynamoDB.
     *
     * @return
     */
    public List<EC2Instance> scanInstanceByClonePath(String clonePath) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        List<EC2Instance> list = new ArrayList<EC2Instance>();
        Table table = dynamoDB.getTable(instanceTableName);
        ScanSpec scanSpec = new ScanSpec().withFilterExpression("info.clonePath = :cp")
                .withValueMap(new ValueMap().withString(":cp", clonePath));
        // .withProjectionExpression("#yr, title, info.rating");

        try {
            ItemCollection<ScanOutcome> items = table.scan(scanSpec);

            Iterator<Item> iter = items.iterator();
            Gson gson = new Gson();
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.debug(item.toJSONPretty());
                EC2Instance instance = gson.fromJson(item.toJSONPretty(), EC2Instance.class);
                list.add(instance);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table:");
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Scan all instance items in DynamoDB.
     *
     * @return
     */
    public List<EC2Instance> queryInstanceById(String instanceId) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        List<EC2Instance> list = new ArrayList<EC2Instance>();
        Table table = dynamoDB.getTable(instanceTableName);
        ScanSpec scanSpec = new ScanSpec();
        // .withProjectionExpression("#yr, title, info.rating");

        HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#ii", "instanceId");

        HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":yyyy", instanceId);

        QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#ii = :yyyy").withNameMap(nameMap)
                .withValueMap(valueMap);


        try {
            ItemCollection<QueryOutcome> items = table.query(querySpec);
            Iterator<Item> iter = items.iterator();
            Gson gson = new Gson();
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.debug(item.toJSONPretty());
                EC2Instance instance = gson.fromJson(item.toJSONPretty(), EC2Instance.class);
                list.add(instance);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table:");
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Create item for ec2 instance.
     *
     * @param instance
     * @return
     */
    public String createInstanceItem(EC2Instance instance) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(instanceTableName);

        final Map<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("startTime", instance.getInfo().getStartTime());
        infoMap.put("clonePath", instance.getInfo().getClonePath());
        infoMap.put("instanceType", instance.getInfo().getInstanceType());
        String message;
        try {
            logger.debug("Adding a new item...");
            PutItemOutcome outcome = table
                    .putItem(new Item().withPrimaryKey("instanceId", instance.getInstanceId()).withMap("info", infoMap));
            message = "PutItem succeeded:\n" + outcome.getPutItemResult();
            logger.debug(message);

        } catch (Exception e) {
            message = "Unable to add item: " + instance.getInstanceId();
            logger.error(message);
            logger.error(e.getMessage());
        }

        return message;
    }

    /**
     * Delete instance item by instance id.
     *
     * @param instanceId
     * @return
     */
    public String deleteInstanceItem(String instanceId) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(instanceTableName);
        String message = "";
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("instanceId", instanceId));
        // .withConditionExpression("info.rating <= :val")
//                .withValueMap(new ValueMap().withNumber(":val", 5.0));

        // Conditional delete (we expect this to fail)
        try {
            logger.debug("Attempting a conditional delete...");
            table.deleteItem(deleteItemSpec);
            message = "DeleteItem succeeded";
            logger.debug(message);
        } catch (Exception e) {
            message = "Unable to add item: " + instanceId;
            logger.error(message);
            logger.error(e.getMessage());
        }
        return message;
    }

    /**
     * Create a DynamoDB table, the table name, partitionKey , sortKey should be String type.
     *
     * @param tableName
     * @param partitionKey
     * @return
     */
    public String createTable(String tableName, String partitionKey) {
        if (dynamoDB == null) {
            logger.info("DynamoDBSingleton.getInstance() ");
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        String message;
        try {
            logger.debug("Attempting to create table; please wait...");
            Table table = dynamoDB.createTable(tableName,
                    Arrays.asList(new KeySchemaElement(partitionKey, KeyType.HASH)),
                    Arrays.asList(new AttributeDefinition(partitionKey, ScalarAttributeType.S)),
                    new ProvisionedThroughput(10L, 10L));
            table.waitForActive();
            message = "Success.  Table status: " + table.getDescription().getTableStatus();
            logger.debug(message);
        } catch (Exception e) {
            logger.error("Unable to create table: ");
            logger.error(e.getMessage());
            message = "Unable to create table: " + e.getMessage();
        }
        return message;
    }

    /**
     * Create item for clone config.
     *
     * @param config
     * @return
     */
    public String createConfigItem(CloneConfig config) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(configTableName);
        String dateTime = FORMAT.format(new java.util.Date());

        final Map<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("awsRegion", config.getInfo().getAwsRegion());
        infoMap.put("amiID", config.getInfo().getAmiID());
        infoMap.put("transferS3LogBucket", config.getInfo().getTransferS3LogBucket());
        infoMap.put("rcloneCommand", config.getInfo().getRcloneCommand());
        infoMap.put("emailFrom", config.getInfo().getEmailFrom());
        infoMap.put("emailTo", config.getInfo().getEmailTo());
        infoMap.put("sqsQueueUrl", config.getInfo().getSqsQueueUrl());
        infoMap.put("updateTime", dateTime);
        infoMap.put("sourceBucketName", config.getInfo().getSourceBucketName());
        infoMap.put("destinationBucketName", config.getInfo().getDestinationBucketName());
        String message;
        try {
            logger.debug("Adding a new config item...");
            PutItemOutcome outcome = table
                    .putItem(new Item().withPrimaryKey("configKey", config.getConfigKey()).withMap("info", infoMap));
            message = "PutItem succeeded:\n" + outcome.getPutItemResult();
            logger.debug(message);

        } catch (Exception e) {
            message = "Unable to add item: " + config.getConfigKey();
            logger.error(message);
            logger.error(e.getMessage());
        }
        return message;
    }

    /**
     * Query config items in DynamoDB by config key.
     *
     * @return
     */
    public List<CloneConfig> queryConfigByKey(String configKey) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        List<CloneConfig> list = new ArrayList<CloneConfig>();
        Table table = dynamoDB.getTable(configTableName);

        HashMap<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("#ii", "configKey");

        HashMap<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put(":yyyy", configKey);

        QuerySpec querySpec = new QuerySpec().withKeyConditionExpression("#ii = :yyyy").withNameMap(nameMap)
                .withValueMap(valueMap);


        try {
            ItemCollection<QueryOutcome> items = table.query(querySpec);
            Iterator<Item> iter = items.iterator();
//            System.out.println("iter.hasNext() : " + iter.hasNext());
            while (iter.hasNext()) {
                Item item = iter.next();
                logger.info(item.toJSONPretty());
                JSONObject jsonObject=JSONObject.fromObject(item.toJSONPretty());
                CloneConfig config=(CloneConfig)JSONObject.toBean(jsonObject, CloneConfig.class);
//                Gson gson = new Gson();
//                JsonParser parser = new JsonParser();
//                JsonObject object = (JsonObject) parser.parse(item.toJSONPretty());
//                CloneConfig config = gson.fromJson(object, CloneConfig.class);
                list.add(config);
            }
        } catch (Exception e) {
            logger.error("Unable to scan the table: " + configTableName);
            logger.error(e.getMessage());
        }
        return list;
    }

    /**
     * Update info for a config by config key
     *
     * @param config
     * @return
     */
    public String updateConfigItem(CloneConfig config) {
        if (dynamoDB == null) {
            dynamoDB = DynamoDBSingleton.getInstance();
        }
        Table table = dynamoDB.getTable(configTableName);
        String message;

        String dateTime = FORMAT.format(new java.util.Date());
        UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("configKey", config.getConfigKey())
                .withUpdateExpression("set info.awsRegion = :i, " +
                        "info.amiID = :s, " +
                        "info.transferS3LogBucket = :d, " +
                        "info.rcloneCommand = :e, " +
                        "info.emailFrom = :u, " +
                        "info.emailTo = :a, " +
                        "info.sqsQueueUrl = :x, " +
                        "info.updateTime = :r, " +
                        "info.sourceBucketName = :sourceBucketName, " +
                        "info.destinationBucketName = :destinationBucketName")
                .withValueMap(new ValueMap().withString(":i", config.getInfo().getAwsRegion())
                        .withString(":s", config.getInfo().getAmiID())
                        .withString(":d", config.getInfo().getTransferS3LogBucket())
                        .withString(":e", config.getInfo().getRcloneCommand())
                        .withString(":u", config.getInfo().getEmailFrom())
                        .withString(":a", config.getInfo().getEmailTo())
                        .withString(":x", config.getInfo().getSqsQueueUrl())
                        .withString(":r", dateTime)
                        .withString(":sourceBucketName", config.getInfo().getSourceBucketName())
                        .withString(":destinationBucketName", config.getInfo().getDestinationBucketName()))
                .withReturnValues(ReturnValue.UPDATED_NEW);

        try {
            logger.debug("Updating the item...");
            UpdateItemOutcome outcome = table.updateItem(updateItemSpec);
            message = "UpdateItem succeeded:\n" + outcome.getItem().toJSONPretty();
            logger.debug(message);
        } catch (Exception e) {
            message = "Unable to update item: " + config.getConfigKey();
            logger.error(message);
            logger.error(e.getMessage());
        }
        return message;
    }


}