package com.company.def.service.cb;

import com.company.def.dto.ChargeBeeRedirectDTO;
import com.chargebee.models.Customer;
import com.chargebee.models.PortalSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;

public interface ChargeBeeCustomerService {

    void saveChargeBeeId(final String chargeBeeId, final Long doctorId);

    void moveInCustomer(final Customer customer) throws Exception;

    PortalSession createPortalSession(final long doctorId, final String redirectUrl) throws Exception;

    boolean isCbIdExists(final long  doctorId);

    ChargeBeeRedirectDTO cbRedirect(final String cbId, String subscriptionId, final HttpServletRequest request, final HttpServletResponse response) throws Exception;

    ChargeBeeRedirectDTO cbRedirectByDoctorId(Long doctorId) throws Exception;

    boolean isCbUserRegistered(final String cbId, final HttpServletRequest request);

    ChargeBeeRedirectDTO upgradeSubscriptionRedirect(final Integer doctorId, final HttpServletRequest request, final HttpServletResponse response) throws MalformedURLException;

    boolean cbIdIsValid(final String cbId) throws Exception;

    boolean customerHasPaymentMethod(String cbId) throws Exception;

    Customer getCustomerFromChargeBee(String cbId) throws Exception;
}
