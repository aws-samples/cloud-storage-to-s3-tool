package org.aws.gcrrp.datatos3.utils;

import org.aws.gcrrp.datatos3.repository.DynamoDBRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDBClientSingleton {

    private static Logger logger = LoggerFactory.getLogger(DDBClientSingleton.class);
    private DDBClientSingleton() {}
    private static class SingletonDDBClient {
        private static final DynamoDBRepository dynamoDBRepository = new DynamoDBRepository();

    }

    public static DynamoDBRepository getInstance() {
        return SingletonDDBClient.dynamoDBRepository;
    }
}
