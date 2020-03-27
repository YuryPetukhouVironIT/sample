package com.company.def.service.cb;

import com.company.def.SlackClient;
import com.company.def.model.chargebee.ChargeBeeEventInfo;
import com.company.def.repository.ChargeBeeEventInfoRepository;
import com.chargebee.models.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ChargeBeeEventTask {

    private static final Logger logger = LogManager.getLogger(ChargeBeeEventTask.class);
    @Autowired
    private ChargeBeeEventInfoRepository repository;
    @Autowired
    private ChargeBeeCustomerService customerService;
    @Autowired
    private ChargeBeeSubscriptionService subscriptionService;

    public ChargeBeeEventTask() {
    }

    @Scheduled(fixedDelay = 3000)
    public void handleEvent() {

        final ChargeBeeEventInfo chargeBeeEventInfo = repository.findFirstBySkippedFalseAndProcessedFalseOrderByResourceVersionAsc();

        if (chargeBeeEventInfo != null) {
            try {
                final Event chargeBeeEvent = new Event(chargeBeeEventInfo.getJson());
                if (chargeBeeEventInfo.getResourceVersion() != null) {
                    processChargeBeeEvent(chargeBeeEvent);
                    chargeBeeEventInfo.setProcessed(true);
                    if (chargeBeeEventInfo.getProcessedDateTime() == null) {
                        chargeBeeEventInfo.setProcessedDateTime(new Date());
                    }
                    repository.save(chargeBeeEventInfo);
                } else {
                    chargeBeeEventInfo.setSkipped(true);
                    if (chargeBeeEventInfo.getProcessedDateTime() == null) {
                        chargeBeeEventInfo.setProcessedDateTime(new Date());
                    }
                    repository.save(chargeBeeEventInfo);
                    logger.warn("Cannot process event " + chargeBeeEvent.id() + ". Null value of resource version");
                }
            } catch (Exception e) {
                final String errorMessage = "Error while processing cb event with DB id " + chargeBeeEventInfo.getId();
                logger.error(errorMessage,e);
                chargeBeeEventInfo.setSkipped(true);
                SlackClient.sendMessageToChannel(errorMessage, SlackClient.urlSupportChannel);
                repository.save(chargeBeeEventInfo);
            }
        }
    }

    private boolean eventInfoResourceVersionIsUnique(final Long resourceVersion) {
        return repository.findFirstByResourceVersionAndProcessedTrueAndResourceVersionIsNotNull(resourceVersion) == null;
    }

    private void processChargeBeeEvent(final Event chargeBeeEvent) throws Exception {
        switch (chargeBeeEvent.eventType()) {
            case CUSTOMER_CREATED: {
                customerService.moveInCustomer(chargeBeeEvent.content().customer());
                logger.info("Customer with cb id "+chargeBeeEvent.content().customer()+" is created while handling cb event");
                break;
            }
            case CUSTOMER_MOVED_IN: {
                customerService.moveInCustomer(chargeBeeEvent.content().customer());
                logger.info("Customer with cb id "+chargeBeeEvent.content().customer()+" is created while handling cb event");
                break;
            }
            case SUBSCRIPTION_CREATED: {
                subscriptionService.createSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_STARTED: {
                subscriptionService.startSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_REACTIVATED: {
                subscriptionService.reactivateSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_RESUMED: {
                subscriptionService.resumeSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_CHANGED: {
                subscriptionService.changeSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_CANCELLED: {
                subscriptionService.cancelSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_DELETED: {
                subscriptionService.deleteSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            case SUBSCRIPTION_PAUSED: {
                subscriptionService.pauseSubscription(chargeBeeEvent.content().subscription());
                break;
            }
            default: {
                logger.warn("Cannot process event " + chargeBeeEvent.id() + ". Wrong event type:" + chargeBeeEvent.eventType());
                break;
            }
        }
        logger.info("CB event " + chargeBeeEvent.id() + " processed: " + chargeBeeEvent.toJson());
    }
}
