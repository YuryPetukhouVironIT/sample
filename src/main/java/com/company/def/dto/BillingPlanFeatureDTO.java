package com.company.def.dto;

import java.util.List;
import java.util.Objects;

public class BillingPlanFeatureDTO {

    private Integer planId;
    private List<Integer> featuresId;
    private String planName;
    private String planCloudName;
    private Integer caseCount;
    private Boolean isFree;

    public BillingPlanFeatureDTO() {
    }

    public Integer getPlanId() {
        return planId;
    }

    public BillingPlanFeatureDTO setPlanId(Integer planId) {
        this.planId = planId;
        return this;
    }

    public List<Integer> getFeaturesId() {
        return featuresId;
    }

    public BillingPlanFeatureDTO setFeaturesId(List<Integer> featuresId) {
        this.featuresId = featuresId;
        return this;
    }

    public String getPlanName() {
        return planName;
    }

    public BillingPlanFeatureDTO setPlanName(String planName) {
        this.planName = planName;
        return this;
    }

    public Integer getCaseCount() {
        return caseCount;
    }

    public BillingPlanFeatureDTO setCaseCount(Integer caseCount) {
        this.caseCount = caseCount;
        return this;
    }

    public Boolean isIsFree() {
        return isFree;
    }

    public BillingPlanFeatureDTO setIsFree(Boolean free) {
        isFree = free;
        return this;
    }

    public String getPlanCloudName() {
        return planCloudName;
    }

    public BillingPlanFeatureDTO setPlanCloudName(String planCloudName) {
        this.planCloudName = planCloudName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillingPlanFeatureDTO)) {
            return false;
        }
        BillingPlanFeatureDTO that = (BillingPlanFeatureDTO) o;
        return Objects.equals(planId, that.planId) &&
            Objects.equals(featuresId, that.featuresId) &&
            Objects.equals(planName, that.planName) &&
            Objects.equals(planCloudName, that.planCloudName) &&
            Objects.equals(caseCount, that.caseCount) &&
            Objects.equals(isFree, that.isFree);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId, featuresId, planName, planCloudName, caseCount, isFree);
    }

    @Override
    public String toString() {
        return "BillingPlanFeatureDTO{" +
            "planId=" + planId +
            ", featuresId=" + featuresId +
            ", planName='" + planName + '\'' +
            ", planCloudName='" + planCloudName + '\'' +
            ", caseCount=" + caseCount +
            ", isFree=" + isFree +
            '}';
    }
}
