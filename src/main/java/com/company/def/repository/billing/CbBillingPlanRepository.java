package com.company.def.repository.billing;

import com.company.def.model.billing.CbBillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbBillingPlanRepository extends JpaRepository <CbBillingPlan, Integer> {
    List<CbBillingPlan> findAllByOrderByNameAsc();
}
