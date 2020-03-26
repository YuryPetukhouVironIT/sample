package com.cephx.def.service.billing;

import com.cephx.def.model.billing.Feature;
import com.cephx.def.repository.billing.FeaturesRepository;
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
