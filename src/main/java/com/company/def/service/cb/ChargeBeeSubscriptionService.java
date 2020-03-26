package com.cephx.def.service.cb;

import com.chargebee.models.Customer;
import com.chargebee.models.Subscription;

public interface ChargeBeeSubscriptionService {
    void createSubscription(final Subscription subscription);

    void resumeSubscription(final Subscription subscription);

    void startSubscription(final Subscription subscription);

    void reactivateSubscription(final Subscription subscription);

    void changeSubscription(final Subscription subscription);

    void cancelSubscription(final Subscription subscription);

    void deleteSubscription(final Subscription subscription);

    void pauseSubscription(final Subscription subscription);

    boolean customerHasActiveSubscription(String cbId) throws Exception;

}
