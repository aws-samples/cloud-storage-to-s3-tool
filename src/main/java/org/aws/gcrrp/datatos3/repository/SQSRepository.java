package org.aws.gcrrp.datatos3.repository;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.web.bind.annotation.GetMapping;
import org.aws.gcrrp.datatos3.model.CloneTask;
import org.aws.gcrrp.datatos3.utils.ENVConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The SQS APIs.
 *
 * @author JiaTing
 * @version 1.0
 * @since 2022-03-13
 */


@Repository
public class SQSRepository {

    static Logger logger = LoggerFactory.getLogger(SQSRepository.class);

    @Autowired
    private AmazonSQS sqs;

    public String listQueues() {

        ListQueuesResult lq_result = sqs.listQueues();
        logger.info("Your SQS Queue URLs:");
        for (String url : lq_result.getQueueUrls()) {
            logger.info(url);
        }
        return "";
    }

    /**
     * Send a message to SQS queue.
     *
     * @param msgBody
     * @return
     */
    @GetMapping("/sendMsgToSQSQueue")
    public String sendMsgToSQSQueue(String msgBody) {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withRegion(Regions.fromName(ENVConfig.awsRegion)).build();
        String resultMessage;
        try {
            SendMessageRequest send_msg_request = new SendMessageRequest()
                    .withQueueUrl(ENVConfig.sqsQueueUrl)
                    .withMessageGroupId("test")
                    .withMessageBody(msgBody)
                    .withMessageDeduplicationId(String.valueOf(System.currentTimeMillis()) + Math.random());
            sqs.sendMessage(send_msg_request);
            resultMessage = "Send message to SQS successfully";
        } catch (Exception e) {
            resultMessage = "Send message to SQS failed";
            logger.error(e.getMessage());
        }
        logger.info(resultMessage);
        return resultMessage;
    }

    /**
     * Receive message from SQS
     *
     * @return
     */
    @GetMapping("/receiveMessageFromSQSQueue")
    public CloneTask receiveMessageFromSQSQueue() {
        String resultMessage;
        CloneTask task = new CloneTask();
        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withRegion(Regions.fromName(ENVConfig.awsRegion)).build();

        List<CloneTask> returnList = new ArrayList<CloneTask>();
        try {
            List<Message> messages = sqs.receiveMessage(ENVConfig.sqsQueueUrl).getMessages();
            logger.debug("Message Count in SQS : " + messages.size());
            if (messages.size() == 0) {
                return null;
            }
            Message m = messages.get(0);
            logger.info("m.getBody(): " + m.getBody());
            Gson gson = new Gson();
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(m.getBody());
            task = gson.fromJson(object, CloneTask.class);
            returnList.add(task);
            sqs.deleteMessage(ENVConfig.sqsQueueUrl, m.getReceiptHandle());

            resultMessage = "Receive message to SQS successfully";
        } catch (Exception e) {
            resultMessage = "Receive message to SQS failed";
            logger.info(e.getMessage());
        }
        logger.debug(resultMessage);
        return task;
    }

    public static void addMessageAttributes(Map<String, MessageAttributeValue> smsAttributes) {
        smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
                .withStringValue("123456") //The sender ID shown on the device.
                .withDataType("String"));
        smsAttributes.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue()
                .withStringValue("0.50") //Sets the max price to 0.50 USD.
                .withDataType("Number"));
        smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
                .withStringValue("Promotional") //Sets the type to promotional.
                .withDataType("String"));
    }

    /**
     * @param queueName
     * @return queueUrl
     * @author jiatin
     */
    public String createQueue(String queueName) {
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.fromName(ENVConfig.awsRegion)).build();
        CreateQueueRequest create_request = new CreateQueueRequest(queueName)
                .addAttributesEntry("FifoQueue", "true")
                .addAttributesEntry("ContentBasedDeduplication", "true");

        String queueUrl = "";
        try {
            CreateQueueResult r = sqs.createQueue(create_request);
            queueUrl = r.getQueueUrl();
            logger.info("Create sqs queue successfully, queue url = " + queueUrl);
        } catch (AmazonSQSException e) {
            if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                throw e;
            }
        }
        return queueUrl;
    }
}
