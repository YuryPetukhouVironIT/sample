package com.company.def.service.cb;

import com.company.def.SlackClient;
import com.company.def.model.chargebee.ChargeBeeEventInfo;
import com.company.def.repository.ChargeBeeEventInfoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChargeBeeEventTaskChecker {

    private static final Logger logger = LogManager.getLogger(ChargeBeeEventTaskChecker.class);
    private static final List<ChargeBeeEventInfo> chargeBeeEventInfos = new ArrayList<>();
    private static boolean queueStuck = false;

    @Autowired
    private ChargeBeeEventInfoRepository repository;

    public ChargeBeeEventTaskChecker() {
    }

    @Scheduled(fixedDelay = 30000)
    public void checkUnprocessedEvents() {
        final List<ChargeBeeEventInfo> chargeBeeEventInfoToCheck = repository.findAllBySkippedFalseAndProcessedFalseOrderByResourceVersionAsc();
        if (chargeBeeEventInfoToCheck.isEmpty() && chargeBeeEventInfos.isEmpty()) {
            queueStuck = false;
            return;
        }
        if (chargeBeeEventInfos.equals(chargeBeeEventInfoToCheck) ) {
            if (!queueStuck) {
                queueStuck = true;
                final String errorMessage = "Possible CB events queue stuck: " + chargeBeeEventInfoToCheck.size() + " tasks unprocessed for 1 min";
                logger.warn(errorMessage);
                SlackClient.sendMessageToChannel(errorMessage, SlackClient.urlSupportChannel);
            }
        } else {
            queueStuck = false;
            chargeBeeEventInfos.clear();
            chargeBeeEventInfos.addAll(chargeBeeEventInfoToCheck);
        }

    }
}
