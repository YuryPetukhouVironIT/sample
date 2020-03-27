package com.company.def.service.billing;

import com.company.def.dto.BillingPlanFeatureDTO;
import com.company.def.model.billing.BillingPlan;
import com.company.def.model.billing.CbBillingPlan;

import java.util.List;

public interface BillingPlanService {
    List<BillingPlan> allPlans();

    void updateBillingPlan(final Long doctorId, final Integer billingPlanId, final String billingPlanType);

    void saveBillingPlans(final List<BillingPlanFeatureDTO> billingPlanFeatureDTO);

    List<CbBillingPlan> allCbPlans() throws Exception;

    Long getNumberOfRemainingUploads(Long doctorId);
}
