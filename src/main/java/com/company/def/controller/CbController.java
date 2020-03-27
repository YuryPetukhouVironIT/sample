package com.company.def.controller;

import com.company.def.dto.ChargeBeeRedirectDTO;
import com.company.def.service.AuthenticationService;
import com.company.def.service.cb.ChargeBeeCustomerService;
import com.company.def.service.cb.ChargeBeeService;
import com.company.def.service.db.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/cb")
public class CbController {
    @Autowired
    private ChargeBeeService cbService;
    @Autowired
    private ChargeBeeCustomerService cbCustomerService;
    @Autowired
    private AuthenticationService authenticationService;

    @RequestMapping("/handleEvent")
    @ResponseBody
    public String handleEvent(@RequestBody String eventString) {
        cbService.receiveEvent(eventString);
        return "";
    }

    @RequestMapping("/createPortalSession")
    @ResponseBody
    public String createPortalSession (@RequestParam final Long doctorId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (DoctorService.getDoctorInfoById(doctorId).user.equals(authenticationService.checkApiTokenAuthentication(request, response))) {
            return cbCustomerService.createPortalSession(doctorId, request.getRequestURL().toString()).jsonObj.toString();
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
    }

    @RequestMapping("/isCbIdExists")
    @ResponseBody
    public boolean isCbIdExists (@RequestParam final Long doctorId,
                                 final HttpServletRequest request,
                                 final HttpServletResponse response) throws Exception {
        if (DoctorService.getDoctorInfoById(doctorId).user.equals(authenticationService.checkApiTokenAuthentication(request, response))) {
            return cbCustomerService.isCbIdExists(doctorId);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @RequestMapping("/isCbUserRegistered")
    @ResponseBody
    public boolean isCbUserRegistered (@RequestParam final String cbId,
                                       final HttpServletRequest request) throws Exception {
        return cbCustomerService.isCbUserRegistered(cbId, request);
    }

    @RequestMapping("/signupRedirect")
    @ResponseBody
    public ChargeBeeRedirectDTO signpRedirect (@RequestParam(required = false) final String hostedPageId,
                                               @RequestParam(required = false) final String subscriptionId,
                                               final HttpServletRequest request,
                                               final HttpServletResponse response) throws Exception {
        return cbCustomerService.cbRedirect(hostedPageId, subscriptionId, request, response);
    }

    @RequestMapping("/upgradeSubscriptionRedirect")
    @ResponseBody
    public ChargeBeeRedirectDTO upgradeSubscriptionRedirect (@RequestParam final Integer doctorId,
                                                             final HttpServletRequest request,
                                                             final HttpServletResponse response) throws Exception {
        return cbCustomerService.upgradeSubscriptionRedirect(doctorId, request, response);
    }

    @RequestMapping("/cbRedirectByDoctorId")
    @ResponseBody
    public ChargeBeeRedirectDTO cbRedirectByDoctorId (@RequestParam final Long doctorId) throws Exception {
        return cbCustomerService.cbRedirectByDoctorId(doctorId);
    }

}
