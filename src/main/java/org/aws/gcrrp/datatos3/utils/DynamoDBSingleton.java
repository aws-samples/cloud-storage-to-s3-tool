package org.aws.gcrrp.datatos3.utils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBSingleton {

    private static Logger logger = LoggerFactory.getLogger(DynamoDBSingleton.class);
    private DynamoDBSingleton() {}
    private static class SingletonDynamoDB {
        private static final DynamoDBSingleton ClientSingleton = new DynamoDBSingleton();
        private static final DynamoDB dynamoDB = new DynamoDB(Regions.fromName(ENVConfig.awsRegion));
    }

    public static DynamoDB getInstance() {
        return SingletonDynamoDB.dynamoDB;
    }
}
