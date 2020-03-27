package com.company.def.model.chargebee;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cb_doctor_id")
public class ChargeBeeDoctorId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "doctor_id")
    private Integer doctorId;

    @Column(name = "cb_id")
    private String chargeBeeId;

    public ChargeBeeDoctorId() {
    }

    public Integer getId() {
        return id;
    }

    public ChargeBeeDoctorId setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public ChargeBeeDoctorId setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
        return this;
    }

    public String getChargeBeeId() {
        return chargeBeeId;
    }

    public ChargeBeeDoctorId setChargeBeeId(String chargeBeeId) {
        this.chargeBeeId = chargeBeeId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChargeBeeDoctorId)) {
            return false;
        }
        ChargeBeeDoctorId that = (ChargeBeeDoctorId) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(doctorId, that.doctorId) &&
            Objects.equals(chargeBeeId, that.chargeBeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, doctorId, chargeBeeId);
    }

    @Override
    public String toString() {
        return "ChargeBeeDoctorId{" +
            "id=" + id +
            ", doctorId=" + doctorId +
            ", chargeBeeId=" + chargeBeeId +
            '}';
    }
}
