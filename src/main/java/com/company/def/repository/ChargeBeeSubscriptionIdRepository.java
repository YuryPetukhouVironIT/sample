package com.company.def.repository;

import com.company.def.model.chargebee.ChargeBeeSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargeBeeSubscriptionIdRepository extends JpaRepository<ChargeBeeSubscriptionId, Integer> {
    boolean existsByChargeBeeId (final String chargeBeeId);
    boolean existsBySubscriptionId(final Integer subscriptionId);
    ChargeBeeSubscriptionId findFirstBySubscriptionId (final Integer subscriptionId);
    ChargeBeeSubscriptionId findFirstByChargeBeeIdOrderBySubscriptionIdDesc (final String chargeBeeId);
}
