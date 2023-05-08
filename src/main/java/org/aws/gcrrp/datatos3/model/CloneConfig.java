package org.aws.gcrrp.datatos3.model;

public class CloneConfig {
    private String configKey;
    private ConfigInfo info;

    public  CloneConfig(){

    }

    public CloneConfig(String configKey, ConfigInfo info) {
        this.configKey = configKey;
        this.info = info;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public ConfigInfo getInfo() {
        return info;
    }

    public void setInfo(ConfigInfo info) {
        this.info = info;
    }

}
