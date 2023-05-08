package org.aws.gcrrp.datatos3.po;

import java.util.Date;

public class Instance {
    private String instanceID;
    private Date createDate;
    private String taskPrifix;

    public Instance(String instanceID, Date createDate) {
        this.instanceID = instanceID;
        this.createDate = createDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    public String getTaskPrifix() {
        return taskPrifix;
    }

    public void setTaskPrifix(String taskPrifix) {
        this.taskPrifix = taskPrifix;
    }
}
