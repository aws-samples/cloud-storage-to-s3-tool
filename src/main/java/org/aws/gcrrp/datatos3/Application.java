package org.aws.gcrrp.datatos3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application {
    static  Logger logger = LoggerFactory.getLogger(Application.class);
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
        logger.info("Application start up");
        // DDB init
        // TODO: 2022/3/18
        //init DDB instancesList todo  query
//        if(list.size != 0 ){
//            new APIController().addDDBEC2List(List);
//            logger.info("DDB init instancesList"+ list.toString);
//        }
    }

}
