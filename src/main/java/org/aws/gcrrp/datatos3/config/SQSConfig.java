package org.aws.gcrrp.datatos3.config;

import com.amazonaws.services.sqs.AmazonSQS;

import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.aws.gcrrp.datatos3.utils.ENVConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SQSConfig {


    @Bean
    public AmazonSQS amazonSQS() {

        AmazonSQS sqs = AmazonSQSClientBuilder.standard()
                .withRegion(ENVConfig.awsRegion).build();

        return sqs;
    }
}
