package org.aws.gcrrp.datatos3.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class EC2ClientSingleton {

//    @Value("${aws.region}")
//    private String region_code;

//    private Region region = Region.of(region_code);

    private static Logger logger = LoggerFactory.getLogger(EC2ClientSingleton.class);
    private EC2ClientSingleton() {}
    private static class SingletonInstance {
        private static final EC2ClientSingleton ClientSingleton = new EC2ClientSingleton();

        private static final  Ec2Client ec2 = Ec2Client.builder()
                .region(ENVConfig.getRegion())
                .build();

    }

    public static Ec2Client getInstance() {
        return SingletonInstance.ec2;
    }
}
