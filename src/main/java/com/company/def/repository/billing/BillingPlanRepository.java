package com.company.def.repository.billing;

import com.company.def.model.billing.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingPlanRepository extends JpaRepository<BillingPlan, Integer> {
    List<BillingPlan> findAll();

    BillingPlan findFirstByName(final String name);
}
