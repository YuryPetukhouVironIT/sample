package com.company.def.controller;

import com.company.def.dto.BillingPlanFeatureDTO;
import com.company.def.funcclass;
import com.company.def.model.billing.BillingPlan;
import com.company.def.model.billing.CbBillingPlan;
import com.company.def.service.AuthenticationService;
import com.company.def.service.billing.BillingPlanService;
import com.company.def.service.billing.FeatureService;
import com.company.def.service.db.DoctorService;
import com.company.def.service.db.PatientService;
import com.company.def.servlets.admin.AdminConsts;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/billingPlans")
public class BillingPlanController {

    @Autowired
    private BillingPlanService billingPlanService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private AuthenticationService authenticationService;

    @RequestMapping("/editBillingPlans")
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView openEditBillingPlansPage() throws Exception {
        final List<BillingPlan> billingPlans = billingPlanService.allPlans();
        final List<CbBillingPlan> cbBillingPlans = billingPlanService.allCbPlans();
        final ModelAndView model = new ModelAndView("admin/editBillingPlans");
        model.addObject("features", featureService.allFeatures());
        model.addObject("featuresJson", new JSONArray(featureService.allFeatures()));
        model.addObject("billingPlans", billingPlans);
        model.addObject("cbBillingPlans", cbBillingPlans);
        return model;
    }

    @RequestMapping("/saveBillingPlans")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String saveBillingPlans(@RequestBody final List<BillingPlanFeatureDTO> billingPlanFeatureDTO, final HttpServletResponse response, final HttpServletRequest request) throws IOException {
        billingPlanService.saveBillingPlans(billingPlanFeatureDTO);
        return funcclass.baseUrl+AdminConsts.PG_START_ADMIN;
    }

    @RequestMapping("/setBillingPlan")
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView setBillingPlans(@RequestParam(name="user_id") final Long userId,
                                        @RequestParam(name="doctor_id") final Long doctorId,
                                        @RequestParam(name="billing_plan_id") final Integer billingPlanId,
                                        @RequestParam(name="billing_plan_type") final String billingPlanType,
                                        @RequestParam(name="auth_token") final String authToken) throws AuthenticationException {
        if (!authenticationService.checkTokenAuthentication(authToken,userId)){
            throw new AuthenticationException();
        }
        billingPlanService.updateBillingPlan(doctorId,billingPlanId, billingPlanType);
        final ModelAndView model = new ModelAndView(AdminConsts.PG_START_ADMIN);
        return model;
    }


    @RequestMapping("/getNumberOfRemainingUploads")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Long getNumberOfRemainingUploads(@RequestParam(name="doctorId") final Long doctorId) {
            return billingPlanService.getNumberOfRemainingUploads(doctorId);
    }

    @RequestMapping("/isCephUploadAllowed")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Boolean isCephUploadAllowed(@RequestParam(name="doctorId") final Long doctorId) throws Exception {
        return DoctorService.isCephUploadAllowed(doctorId);
    }

    @RequestMapping("/isDicomUploadAllowed")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Boolean isDicomUploadAllowed(@RequestParam(name="doctorId") final Long doctorId) throws Exception {
        return DoctorService.isDicomUploadAllowed(doctorId);
    }


}
