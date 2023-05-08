package org.aws.gcrrp.datatos3.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.aws.gcrrp.datatos3.utils.ENVConfig;

@Configuration
public class DynamoDBConfig {

//    @Value("${aws.access-key}")
//    private String awsAccessKey;
//
//    @Value("${aws.secret-key}")
//    private String awsSecretKey;

//    @Value("${aws.region}")
//    private String region;

    @Bean
    public DynamoDB dynamoDB() {
//        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
//                new BasicAWSCredentials(awsAccessKey, awsSecretKey));

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(ENVConfig.awsRegion)
//                .withCredentials(credentialsProvider)
                .build();

        return new DynamoDB(client);
    }
}
