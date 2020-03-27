package com.company.def.model.chargebee;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cb_redirected")
public class ChargeBeeUserRedirected {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cb_id")
    private String chargeBeeId;

    @Column(name = "redirected")
    private Boolean redirected;

    public ChargeBeeUserRedirected() {
    }

    public ChargeBeeUserRedirected(final String chargeBeeId, final Boolean redirected) {
        this.chargeBeeId = chargeBeeId;
        this.redirected = redirected;
    }

    public Integer getId() {
        return id;
    }

    public ChargeBeeUserRedirected setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getChargeBeeId() {
        return chargeBeeId;
    }

    public ChargeBeeUserRedirected setChargeBeeId(String chargeBeeId) {
        this.chargeBeeId = chargeBeeId;
        return this;
    }

    public Boolean getRedirected() {
        return redirected;
    }

    public ChargeBeeUserRedirected setRedirected(Boolean redirected) {
        this.redirected = redirected;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChargeBeeUserRedirected)) {
            return false;
        }
        ChargeBeeUserRedirected that = (ChargeBeeUserRedirected) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(chargeBeeId, that.chargeBeeId) &&
            Objects.equals(redirected, that.redirected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chargeBeeId, redirected);
    }

    @Override
    public String toString() {
        return "ChargeBeeUserRedirected{" +
            "id=" + id +
            ", chargeBeeId='" + chargeBeeId + '\'' +
            ", redirected=" + redirected +
            '}';
    }
}
