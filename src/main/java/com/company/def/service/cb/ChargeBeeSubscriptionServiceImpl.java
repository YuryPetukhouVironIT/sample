package com.company.def.service.cb;

import com.company.def.DBconnection;
import com.company.def.ZohoCrmClient;
import com.company.def.exceptions.NoDoctorWithCbIdException;
import com.company.def.funcclass;
import com.company.def.model.chargebee.ChargeBeeDoctorId;
import com.company.def.model.chargebee.ChargeBeeSubscriptionId;
import com.company.def.repository.ChargeBeeDoctorIdRepository;
import com.company.def.repository.ChargeBeeSubscriptionIdRepository;
import com.company.def.service.db.DoctorService;
import com.chargebee.Environment;
import com.chargebee.ListResult;
import com.chargebee.exceptions.InvalidRequestException;
import com.chargebee.models.Customer;
import com.chargebee.models.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ChargeBeeSubscriptionServiceImpl implements ChargeBeeSubscriptionService {

    private static Map<String, String> billingPeriodMap;
    public static final Object subscriptionCreationLock = new Object();

    static {
        initBillingPeriodMap();
    }

    @Autowired
    private ChargeBeeDoctorIdRepository doctorIdRepository;
    @Autowired
    private ChargeBeeSubscriptionIdRepository subscriptionIdRepository;
    @Autowired
    private ChargeBeeCustomerService chargeBeeCustomerService;

    public ChargeBeeSubscriptionServiceImpl() {
        Environment.configure(funcclass.CB_SITE_NAME, funcclass.CB_API_KEY);
    }

    private static void initBillingPeriodMap() {
        billingPeriodMap = new HashMap<>();
        billingPeriodMap.put("month", "monthly");
        billingPeriodMap.put("year", "annual");
    }

    @Override
    public void createSubscription(final Subscription subscription) {
        synchronized (subscriptionCreationLock) {
            final Integer doctorId = doctorId(subscription);
            final Integer dbSubscriptionId = DBconnection.GetDBconnection().addDoctorBillingPlan(doctorId, subscription.planId(), billingPeriodMap.get(subscription.billingPeriodUnit().toString().toLowerCase()));
            saveSubscriptionId(subscription.id(), dbSubscriptionId);
            if (!"free_plan".equals(subscription.planId().toLowerCase()) && (funcclass.isProdEnvironment() || funcclass.isQaEnvironment())) {
                final Map<String, String> parameters = zohoLeadConversionParameters(doctorId);
                ZohoCrmClient.convertLeadRecordToAccount(parameters, doctorId);
            }
        }
    }

    private Map<String, String> zohoLeadConversionParameters(final Integer doctorId) {
        final Map<String, String> parameters =  new HashMap<>();
        parameters.put("createPotential", "false");
        parameters.put("assignTo", DoctorService.getDoctorEmail(doctorId));
        parameters.put("notifyLeadOwner", "true");
        parameters.put("notifyNewEntityOwner", "true");
        return parameters;
    }

    private void saveSubscriptionId(final String chargeBeeId, final Integer dbSubscriptionId) {
        final ChargeBeeSubscriptionId subscriptionId = subscriptionIdRepository.findFirstByChargeBeeIdOrderBySubscriptionIdDesc(chargeBeeId);
        if (subscriptionId == null) {
            final ChargeBeeSubscriptionId chargeBeeSubscriptionId = new ChargeBeeSubscriptionId();
            chargeBeeSubscriptionId.setChargeBeeId(chargeBeeId);
            chargeBeeSubscriptionId.setSubscriptionId(dbSubscriptionId);
            subscriptionIdRepository.save(chargeBeeSubscriptionId);
        } else {
            subscriptionId.setSubscriptionId(dbSubscriptionId);
            subscriptionIdRepository.save(subscriptionId);
        }
    }

    @Override
    public void resumeSubscription(final Subscription subscription) {
        createSubscription(subscription);
    }

    @Override
    public void startSubscription(final Subscription subscription) {
        createSubscription(subscription);
    }

    @Override
    public void reactivateSubscription(final Subscription subscription) {
        createSubscription(subscription);
    }

    @Override
    public void changeSubscription(final Subscription subscription) {
        createSubscription(subscription);
    }

    @Override
    public void cancelSubscription(final Subscription subscription) {
        final Integer doctorId = doctorId(subscription);
        final Integer dbSubscriptionId = DBconnection.GetDBconnection().addDoctorBillingPlan(doctorId, "free", billingPeriodMap.get(subscription.billingPeriodUnit().toString().toLowerCase()));
        saveSubscriptionId(subscription.id(), dbSubscriptionId);
    }

    @Override
    public void deleteSubscription(final Subscription subscription) {
        cancelSubscription(subscription);
    }

    @Override
    public void pauseSubscription(final Subscription subscription) {
        cancelSubscription(subscription);
    }

    private Integer doctorId(final Subscription subscription) {
        Integer doctorId = null;
        final List<ChargeBeeDoctorId> chargeBeeDoctorIds = doctorIdRepository.findChargeBeeDoctorIdsByChargeBeeId(subscription.customerId());
        if (chargeBeeDoctorIds.isEmpty()) {
            throw new NoDoctorWithCbIdException(subscription.customerId());
        }
        doctorId = chargeBeeDoctorIds.get(0).getDoctorId();
        if (chargeBeeDoctorIds.size() > 1) {
            for (ChargeBeeDoctorId chargeBeeDoctorId : chargeBeeDoctorIds) {
                if (chargeBeeDoctorId.getDoctorId() > doctorId) {
                    doctorId = chargeBeeDoctorId.getDoctorId();
                }
            }
        }
        return doctorId;
    }

    @Override
    public boolean customerHasActiveSubscription(final String cbId) throws Exception {
        try {
            final ListResult result = Subscription.list()
                .customerId().is(cbId)
                .includeDeleted(false)
                .request();
            boolean hasActiveSubscription = false;
            final Iterator<ListResult.Entry> it = result.iterator();
            final Timestamp currentTimeStamp = new Timestamp(new Date().getTime());
            final Customer customer = chargeBeeCustomerService.getCustomerFromChargeBee(cbId);
            while (it.hasNext() && !hasActiveSubscription) {
                final Subscription subscription = it.next().subscription();
                hasActiveSubscription = subscription.nextBillingAt().after(currentTimeStamp) && (subscription.paymentSourceId() != null || customer.paymentMethod()!=null);
            }
            return hasActiveSubscription;
        } catch (InvalidRequestException e) {
            return false;
        }
    }
}
