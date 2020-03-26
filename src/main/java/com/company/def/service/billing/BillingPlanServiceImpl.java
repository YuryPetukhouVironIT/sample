package com.cephx.def.service.billing;

import com.cephx.def.DBconnection;
import com.cephx.def.dto.BillingPlanFeatureDTO;
import com.cephx.def.model.billing.BillingPlan;
import com.cephx.def.model.billing.CbBillingPlan;
import com.cephx.def.model.billing.Feature;
import com.cephx.def.repository.billing.BillingPlanRepository;
import com.cephx.def.repository.billing.CbBillingPlanRepository;
import com.cephx.def.repository.billing.FeaturesRepository;
import com.cephx.def.service.cb.ChargeBeeService;
import com.cephx.def.service.db.DoctorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BillingPlanServiceImpl implements BillingPlanService {

    private static final Logger logger = LogManager.getLogger(BillingPlanServiceImpl.class);
    private static final DBconnection dbConnection = DBconnection.GetDBconnection();

    @Autowired
    private BillingPlanRepository repository;
    @Autowired
    private CbBillingPlanRepository cbPlanRepository;
    @Autowired
    private FeaturesRepository featuresRepository;
    @Autowired
    private ChargeBeeService chargeBeeService;

    @Override
    public List<BillingPlan> allPlans() {
        return repository.findAll();
    }

    @Override
    public void updateBillingPlan(final Long doctorId, final Integer billingPlanId, final String billingPlanType) {
        dbConnection.addDoctorBillingPlan(doctorId, repository.findOne(billingPlanId).getName(), billingPlanType);
    }

    @Override
    public void saveBillingPlans(final List<BillingPlanFeatureDTO> billingPlanFeatureDTO) {
        final List<Integer> planIds = new ArrayList<>();
        for (BillingPlanFeatureDTO billingPlanDTO : billingPlanFeatureDTO) {
            BillingPlan billingPlan = null;
            if (billingPlanDTO.getPlanId() != null) {
                billingPlan = repository.findOne(billingPlanDTO.getPlanId());
            } else {
                billingPlan = new BillingPlan();
                billingPlan.setDate(new Date());
            }
            billingPlan.setName(billingPlanDTO.getPlanName());
            billingPlan.setCloudName(billingPlanDTO.getPlanCloudName());
            billingPlan.setCaseCount(billingPlanDTO.getCaseCount());
            billingPlan.setFree(billingPlanDTO.isIsFree());
            final List<Feature> features = (billingPlanDTO.isIsFree() != null && billingPlanDTO.isIsFree()) ? new ArrayList<Feature>() : featuresRepository.findAll(billingPlanDTO.getFeaturesId());
            billingPlan.setFeatures(features);
            repository.save(billingPlan);
            planIds.add(billingPlan.getId());
        }
        final List<Integer> plansToDeleteIds = new ArrayList<>();
        final List<BillingPlan> allPlans = repository.findAll();
        for (BillingPlan plan : allPlans) {
            plansToDeleteIds.add(plan.getId());
        }
        plansToDeleteIds.removeAll(planIds);
        repository.delete(repository.findAll(plansToDeleteIds));
    }


    @Override
    public List<CbBillingPlan> allCbPlans() throws Exception {
        List<CbBillingPlan> cbPlans = cbPlanRepository.findAllByOrderByNameAsc();
        final List<String> unsavedPlanNames = getUnsavedPlanNames(cbPlans);
        if (!unsavedPlanNames.isEmpty()) {
            cbPlans = syncCbAndSavedPlans(unsavedPlanNames);
        }
        return cbPlans;
    }

    @Override
    public Long getNumberOfRemainingUploads(final Long doctorId) {
        final BillingPlan billingPlan = repository.findFirstByName(dbConnection.getBillingPlanByDoctorId(String.valueOf(doctorId)).getName());
        if (billingPlan!=null) {
            final List<String> billingPlanFeatures = dbConnection.getFeaturesNames(doctorId);
            long numberOfMonthUploads = 0;
            final Date monthStartDate = DoctorService.getBillingPlanMonthStartDate(doctorId);
            if (billingPlanFeatures.contains("3D cases")) {
                numberOfMonthUploads = dbConnection.getCasesCountFromDate(doctorId, "3D", new java.sql.Date(monthStartDate.getTime()));
            } else {
                numberOfMonthUploads = dbConnection.getCasesCountFromDate(doctorId, "2D", new java.sql.Date(monthStartDate.getTime()));
            }
            return billingPlan.getCaseCount() - numberOfMonthUploads>0 ? billingPlan.getCaseCount() - numberOfMonthUploads : 0;
        } else {
            return null;
        }
    }

    private List<CbBillingPlan> syncCbAndSavedPlans(final List<String> unsavedPlanNames) {
        for (String unsavedPlanName : unsavedPlanNames) {
            final CbBillingPlan billingPlan = new CbBillingPlan();
            billingPlan.setName(unsavedPlanName);
            cbPlanRepository.save(billingPlan);
            logger.info("Plan with name " + unsavedPlanName + " is received from cb and saved as new to database with id "+billingPlan.getId());
        }
        return cbPlanRepository.findAllByOrderByNameAsc();
    }

    private List<String> getUnsavedPlanNames(final List<CbBillingPlan> savedPlans) throws Exception {
        final List<String> savedPlanNames = new ArrayList<>();
        for (CbBillingPlan billingPlan : savedPlans) {
            savedPlanNames.add(billingPlan.getName());
        }
        final List<String> cbBillingPlanNames = chargeBeeService.cbBillingPlanNames();
            final List<String> unsavedPlanNames = new ArrayList<>();
        for (String cbBillingPlanName : cbBillingPlanNames) {
            if (!savedPlanNames.contains(cbBillingPlanName)) {
                unsavedPlanNames.add(cbBillingPlanName);
            }
        }
        return unsavedPlanNames;
    }
}
