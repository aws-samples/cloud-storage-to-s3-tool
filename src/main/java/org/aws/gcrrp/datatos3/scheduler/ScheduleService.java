package org.aws.gcrrp.datatos3.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.aws.gcrrp.datatos3.controller.APIController;


@Component
public class ScheduleService {


    final static Logger logger =  LogManager.getLogger(ScheduleService.class);
    //60s一次轮训列表
    @Scheduled(fixedRate = 60000)
    public void printEC2List(){
        logger.debug("invoke printEC2List()");
        new APIController().printList();
    }

    //60s一次轮训列表
    @Scheduled(fixedRate = 60000)
    public void checkComplete(){
        logger.debug("invoke checkComplete()");
        new APIController().checkComplete();
    }


}
