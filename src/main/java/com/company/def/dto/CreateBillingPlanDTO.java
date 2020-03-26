package com.cephx.def.dto;

import java.util.List;

public class CreateBillingPlanDTO {

    private List<Integer> featuresId;

    public CreateBillingPlanDTO() {
    }

    public List<Integer> getFeaturesId() {
        return featuresId;
    }

    public CreateBillingPlanDTO setFeaturesId(List<Integer> featuresId) {
        this.featuresId = featuresId;
        return this;
    }


}
