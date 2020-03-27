package com.company.def.repository;

import com.company.def.model.chargebee.ChargeBeeDoctorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeBeeDoctorIdRepository extends JpaRepository<ChargeBeeDoctorId, Integer> {
    List<ChargeBeeDoctorId> findChargeBeeDoctorIdsByChargeBeeId(final String chargeBeeId);
    List<ChargeBeeDoctorId> findChargeBeeDoctorIdsByDoctorId(final Integer doctorId);
}
