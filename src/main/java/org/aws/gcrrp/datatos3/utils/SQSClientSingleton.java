package org.aws.gcrrp.datatos3.utils;

import org.aws.gcrrp.datatos3.repository.SQSRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQSClientSingleton {

    private static Logger logger = LoggerFactory.getLogger(SQSClientSingleton.class);
    private SQSClientSingleton() {}
    private static class SingletonDDBClient {
        private static final SQSRepository sQSRepository = new SQSRepository();

    }

    public static SQSRepository getInstance() {
        return SingletonDDBClient.sQSRepository;
    }
}
