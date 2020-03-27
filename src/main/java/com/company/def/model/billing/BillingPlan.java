package com.company.def.model.billing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "billing_plans")
public class BillingPlan {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @Column(name = "date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    @Column(name = "name")
    private String name;

    @Column(name = "cloud_name")
    private String cloudName;

    @Column(name = "case_count")
    private int caseCount;

    @Column(name = "is_free")
    private boolean isFree;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "features_billing_plans",
        joinColumns = @JoinColumn (name = "billing_plan_id"),
        inverseJoinColumns = { @JoinColumn(name = "feature_id")}
    )
    private List<Feature> features;

    public BillingPlan() {
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(final int caseCount) {
        this.caseCount = caseCount;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(final List<Feature> features) {
        this.features = features;
    }

    public String getCloudName() {
        return cloudName;
    }

    public BillingPlan setCloudName(String cloudName) {
        this.cloudName = cloudName;
        return this;
    }

    public boolean isFree() {
        return isFree;
    }

    public BillingPlan setFree(boolean free) {
        isFree = free;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillingPlan)) {
            return false;
        }
        BillingPlan that = (BillingPlan) o;
        return id == that.id &&
            caseCount == that.caseCount &&
            isFree == that.isFree &&
            Objects.equals(date, that.date) &&
            Objects.equals(name, that.name) &&
            Objects.equals(cloudName, that.cloudName) &&
            Objects.equals(features, that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, name, cloudName, caseCount, isFree, features);
    }

    @Override
    public String toString() {
        return "BillingPlan{" +
            "id=" + id +
            ", date=" + date +
            ", name='" + name + '\'' +
            ", cloudName='" + cloudName + '\'' +
            ", caseCount=" + caseCount +
            ", isFree=" + isFree +
            ", features=" + features +
            '}';
    }

    public String toJSON() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}
