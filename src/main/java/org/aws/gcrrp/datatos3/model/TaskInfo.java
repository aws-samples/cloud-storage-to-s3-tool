package org.aws.gcrrp.datatos3.model;

public class TaskInfo {
    private String instanceId;
    private String sourceLocation;
    private String destinationLocation;
    private String insertTime;
    private String updateTime;
    private String startCloneTime;
    private String taskStatus;
    private String s3LogUri;
    private String transferSizeAndRate;
    private String transferFileCount;
    private String transferElapsedTime;

    public TaskInfo(String instanceId, String sourceLocation, String destinationLocation, String insertTime, String updateTime, String startCloneTime, String taskStatus, String s3LogUri) {
        this.instanceId = instanceId;
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.insertTime = insertTime;
        this.updateTime = updateTime;
        this.startCloneTime = startCloneTime;
        this.taskStatus = taskStatus;
        this.s3LogUri = s3LogUri;
        this.transferSizeAndRate = "N/A";
        this.transferFileCount = "N/A";
        this.transferElapsedTime = "N/A";
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(String insertTime) {
        this.insertTime = insertTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getStartCloneTime() {
        return startCloneTime;
    }

    public void setStartCloneTime(String startCloneTime) {
        this.startCloneTime = startCloneTime;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getS3LogUri() {
        return s3LogUri;
    }

    public void setS3LogUri(String s3LogUri) {
        this.s3LogUri = s3LogUri;
    }

    public String getTransferSizeAndRate() {
        return transferSizeAndRate;
    }

    public void setTransferSizeAndRate(String transferSizeAndRate) {
        this.transferSizeAndRate = transferSizeAndRate;
    }

    public String getTransferFileCount() {
        return transferFileCount;
    }

    public void setTransferFileCount(String transferFileCount) {
        this.transferFileCount = transferFileCount;
    }

    public String getTransferElapsedTime() {
        return transferElapsedTime;
    }

    public void setTransferElapsedTime(String transferElapsedTime) {
        this.transferElapsedTime = transferElapsedTime;
    }
}

