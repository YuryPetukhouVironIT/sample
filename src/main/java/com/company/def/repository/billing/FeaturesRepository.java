package com.cephx.def.repository.billing;

import com.cephx.def.model.billing.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeaturesRepository extends JpaRepository<Feature, Integer> {
}
