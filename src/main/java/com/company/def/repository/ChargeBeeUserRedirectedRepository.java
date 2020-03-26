package com.cephx.def.repository;

import com.cephx.def.model.chargebee.ChargeBeeUserRedirected;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargeBeeUserRedirectedRepository extends JpaRepository<ChargeBeeUserRedirected, Integer> {
    ChargeBeeUserRedirected getChargeBeeUserRedirectedByChargeBeeId (final String chargeBeeId);
}
