package com.company.def.model.chargebee;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cb_subscription_id")
public class ChargeBeeSubscriptionId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cb_id")
    private String chargeBeeId;

    @Column(name = "subscription_id")
    private Integer subscriptionId;

    public ChargeBeeSubscriptionId() {
    }

    public Integer getId() {
        return id;
    }

    public ChargeBeeSubscriptionId setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getChargeBeeId() {
        return chargeBeeId;
    }

    public ChargeBeeSubscriptionId setChargeBeeId(String chargeBeeId) {
        this.chargeBeeId = chargeBeeId;
        return this;
    }

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public ChargeBeeSubscriptionId setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChargeBeeSubscriptionId)) {
            return false;
        }
        ChargeBeeSubscriptionId that = (ChargeBeeSubscriptionId) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(chargeBeeId, that.chargeBeeId) &&
            Objects.equals(subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chargeBeeId, subscriptionId);
    }

    @Override
    public String toString() {
        return "ChargeBeeSubscriptionId{" +
            "id=" + id +
            ", chargeBeeId='" + chargeBeeId + '\'' +
            ", subscriptionId='" + subscriptionId + '\'' +
            '}';
    }
}
