package org.aws.gcrrp.datatos3.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;

public class SsmClientSingleton {
    private static Logger logger = LoggerFactory.getLogger(SsmClientSingleton.class);
    private SsmClientSingleton() {}

    private static class SingletonInstance {
        private static final SsmClientSingleton ClientSingleton = new SsmClientSingleton();
        private static final SsmClient ssmClient = SsmClient.builder()
                .region(ENVConfig.getRegion())
                .build();
    }

    public static SsmClient getInstance() {
        return SingletonInstance.ssmClient;
    }
}
