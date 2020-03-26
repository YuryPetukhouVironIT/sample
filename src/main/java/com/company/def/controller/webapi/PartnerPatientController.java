package com.cephx.def.controller.webapi;

import com.cephx.def.service.webapi.PartnerPatientService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/webapi")
public class PartnerPatientController {

    @Autowired
    private PartnerPatientService partnerPatientService;

    @GetMapping("/newpatupload")
    public ModelAndView uploadNewPatient(@RequestParam(name="auth_token") final String authToken,
                                         @RequestParam(name="partner_id") final String partnerId,
                                         @RequestParam(name="ext_doctor_id") final long externalDoctorId,
                                         @RequestParam(name="ext_patient_id") final long externalPatientId,
                                         @RequestParam(name="redirect_url") final String redirectUrl,
                                         final HttpServletRequest request,
                                         final HttpServletResponse response) throws Exception {
        partnerPatientService.checkIfPatientExists(request);
        partnerPatientService.passInputParameters(request, response);
        return new ModelAndView("newpatupload");
    }

    @RequestMapping("/patientdownloadpage")
    public ModelAndView analysisDownload(@RequestParam(name="auth_token") final String authToken,
                                         @RequestParam(name="partner_id") final String partnerId,
                                         @RequestParam(name="ext_doctor_id") final long externalDoctorId,
                                         @RequestParam(name="ext_patient_id") final long externalPatientId,
                                         @RequestParam(name="redirect_url") final String redirectUrl,
                                         final HttpServletRequest request,
                                         final HttpServletResponse response) throws Exception {
        partnerPatientService.checkPatient(request);
        partnerPatientService.passInputParameters(request, response);
        final String replyPage = StringUtils.isBlank(redirectUrl) ? partnerPatientService.downloadPage(request) : redirectUrl;
        return new ModelAndView(replyPage);
    }

}
