package org.aws.gcrrp.datatos3.model;

import java.text.SimpleDateFormat;

public class ConfigInfo {
    private String awsRegion;
    private String amiID;
    private String transferS3LogBucket;
    private String rcloneCommand;
    private String emailFrom;
    private String emailTo;
    private String sqsQueueUrl;
    private String updateTime;
    private String sourceBucketName;
    private String destinationBucketName;

    SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ConfigInfo() {

    }

    public ConfigInfo(String awsRegion, String amiID, String transferS3LogBucket, String rcloneCommand, String emailFrom, String emailTo, String sqsQueueUrl, String gcsPath, String s3Path) {
        this.awsRegion = awsRegion;
        this.amiID = amiID;
        this.transferS3LogBucket = transferS3LogBucket;
        this.rcloneCommand = rcloneCommand;
        this.emailFrom = emailFrom;
        this.emailTo = emailTo;
        this.sqsQueueUrl = sqsQueueUrl;
        String dateTime = FORMAT.format(new java.util.Date());
        this.updateTime = dateTime;
        this.sourceBucketName = gcsPath;
        this.destinationBucketName = s3Path;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAmiID() {
        return amiID;
    }

    public void setAmiID(String amiID) {
        this.amiID = amiID;
    }

    public String getTransferS3LogBucket() {
        return transferS3LogBucket;
    }

    public void setTransferS3LogBucket(String transferS3LogBucket) {
        this.transferS3LogBucket = transferS3LogBucket;
    }

    public String getRcloneCommand() {
        return rcloneCommand;
    }

    public void setRcloneCommand(String rcloneCommand) {
        this.rcloneCommand = rcloneCommand;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public String getSqsQueueUrl() {
        return sqsQueueUrl;
    }

    public void setSqsQueueUrl(String sqsQueueUrl) {
        this.sqsQueueUrl = sqsQueueUrl;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getSourceBucketName() {
        return sourceBucketName;
    }

    public void setSourceBucketName(String sourceBucketName) {
        this.sourceBucketName = sourceBucketName;
    }

    public String getDestinationBucketName() {
        return destinationBucketName;
    }

    public void setDestinationBucketName(String destinationBucketName) {
        this.destinationBucketName = destinationBucketName;
    }

}
