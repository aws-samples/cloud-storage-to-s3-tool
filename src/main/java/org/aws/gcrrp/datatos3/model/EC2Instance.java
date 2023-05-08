package org.aws.gcrrp.datatos3.model;

public class EC2Instance {
    private String instanceId;
    private InstanceInfo info;


    public EC2Instance(String instanceId, String instanceType, String jobId, String startTime) {
        this.instanceId = instanceId;
        this.info = new InstanceInfo();
        this.info.setClonePath(jobId);
        this.info.setStartTime(startTime);
        this.info.setInstanceType(instanceType);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public InstanceInfo getInfo() {
        return info;
    }

    public void setInfo(InstanceInfo info) {
        this.info = info;
    }
}

