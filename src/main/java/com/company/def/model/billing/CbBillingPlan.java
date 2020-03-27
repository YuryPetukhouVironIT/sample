package com.company.def.model.billing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cb_billing_plans")
public class CbBillingPlan {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    public Long getId() {
        return id;
    }

    public CbBillingPlan setId(final Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CbBillingPlan setName(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CbBillingPlan)) {
            return false;
        }
        CbBillingPlan that = (CbBillingPlan) o;
        return id == that.id &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "CbBillingPlan{" +
            "id=" + id +
            ", name='" + name + '\'' +
            '}';
    }
}
