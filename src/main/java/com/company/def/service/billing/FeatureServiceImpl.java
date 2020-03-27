package com.company.def.service.billing;

import com.company.def.model.billing.Feature;
import com.company.def.repository.billing.FeaturesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureServiceImpl implements FeatureService {

    @Autowired
    private FeaturesRepository repository;

    @Override
    public List<Feature> allFeatures() {
        return repository.findAll();
    }
}
