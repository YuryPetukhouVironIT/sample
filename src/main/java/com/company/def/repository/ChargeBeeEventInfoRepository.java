package com.company.def.repository;

import com.company.def.model.chargebee.ChargeBeeEventInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeBeeEventInfoRepository extends JpaRepository<ChargeBeeEventInfo, Integer> {
    ChargeBeeEventInfo findFirstBySkippedFalseAndProcessedFalseOrderByResourceVersionAsc();

    List<ChargeBeeEventInfo> findAllBySkippedFalseAndProcessedFalseOrderByResourceVersionAsc();

    ChargeBeeEventInfo findFirstByResourceVersionAndProcessedTrueAndResourceVersionIsNotNull(final long resourceVersion);
}
