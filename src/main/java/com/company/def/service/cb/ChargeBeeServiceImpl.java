package com.cephx.def.service.cb;

import com.cephx.def.funcclass;
import com.cephx.def.model.chargebee.ChargeBeeEventInfo;
import com.cephx.def.repository.ChargeBeeEventInfoRepository;
import com.chargebee.Environment;
import com.chargebee.ListResult;
import com.chargebee.models.Event;
import com.chargebee.models.Plan;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ChargeBeeServiceImpl implements ChargeBeeService {

    private static final Logger logger = LogManager.getLogger(ChargeBeeServiceImpl.class);

    @Autowired
    private ChargeBeeEventInfoRepository repository;

    public ChargeBeeServiceImpl() {
        Environment.configure(funcclass.CB_SITE_NAME, funcclass.CB_API_KEY);
    }

    @Override
    public void receiveEvent(final String eventString) {
        final Event event = new Event(eventString);
        logger.info("CB event " + event.id() + " received: " + event.toJson());
        final ChargeBeeEventInfo chargeBeeEventInfo = new ChargeBeeEventInfo();
        chargeBeeEventInfo.setReceivedDateTime(new Date());
        chargeBeeEventInfo.setProcessed(false);
        chargeBeeEventInfo.setSkipped(false);
        chargeBeeEventInfo.setJson(event.toJson());
        chargeBeeEventInfo.setResourceVersion(resourceVersion(event));
        chargeBeeEventInfo.setOccuredAt(event.occurredAt().getTime());
        repository.save(chargeBeeEventInfo);
        logger.info("CB event " + event.id() + " inserted to DB: " + event.toJson());
    }

    private Long resourceVersion(final Event event) {
        Long resourceVersion = null;
        if (event.content() != null) {
            if (event.content().customer() != null) {
                resourceVersion = event.content().customer().resourceVersion();
            }
            if (event.content().subscription() != null) {
                resourceVersion = event.content().subscription().resourceVersion();
            }
        }
        return resourceVersion;
    }

    @Override
    public List<String> cbBillingPlanNames() throws Exception {
        final List<String> planNames = new ArrayList<>();
        final ListResult result = Plan.list()
            .limit(100)
            .status().is(Plan.Status.ACTIVE)
            .request();
        for (ListResult.Entry entry : result) {
            planNames.add(entry.plan().id());
        }
        return planNames;
    }
}
