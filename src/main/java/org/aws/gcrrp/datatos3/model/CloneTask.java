package org.aws.gcrrp.datatos3.model;

public class CloneTask {
    private String clonePath;

    private TaskInfo info;

    public CloneTask() {

    }

    public CloneTask(String clonePath, String gcsBucket, String s3Bucket, String timeStr) {
        this.clonePath = clonePath;
        TaskInfo taskInfo = new TaskInfo("Not created", gcsBucket + clonePath,
                s3Bucket + clonePath,
                timeStr,
                timeStr,
                timeStr,
                "created",
                "empty");
        this.info = taskInfo;
    }

    public String getClonePath() {
        return clonePath;
    }

    public void setClonePath(String clonePath) {
        this.clonePath = clonePath;
    }


    public TaskInfo getInfo() {
        return info;
    }

    public void setInfo(TaskInfo info) {
        this.info = info;
    }
}
