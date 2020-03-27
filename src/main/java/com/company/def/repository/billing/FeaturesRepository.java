package com.company.def.repository.billing;

import com.company.def.model.billing.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeaturesRepository extends JpaRepository<Feature, Integer> {
}
