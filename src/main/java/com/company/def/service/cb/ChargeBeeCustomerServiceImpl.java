package com.company.def.service.cb;

import com.company.def.BasicDoc;
import com.company.def.DBconnection;
import com.company.def.dto.ChargeBeeRedirectDTO;
import com.company.def.funcclass;
import com.company.def.model.chargebee.ChargeBeeDoctorId;
import com.company.def.model.chargebee.ChargeBeeUserRedirected;
import com.company.def.repository.ChargeBeeDoctorIdRepository;
import com.company.def.repository.ChargeBeeSubscriptionIdRepository;
import com.company.def.repository.ChargeBeeUserRedirectedRepository;
import com.company.def.service.AccountService;
import com.company.def.service.db.DoctorService;
import com.chargebee.Environment;
import com.chargebee.Result;
import com.chargebee.exceptions.InvalidRequestException;
import com.chargebee.models.Customer;
import com.chargebee.models.HostedPage;
import com.chargebee.models.PortalSession;
import com.chargebee.models.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service
public class ChargeBeeCustomerServiceImpl implements ChargeBeeCustomerService {

    private static final Logger logger = LogManager.getLogger(ChargeBeeCustomerServiceImpl.class);
    private static final Object userCreationLock = new Object();
    @Autowired
    private ChargeBeeDoctorIdRepository chargeBeeDoctorIdRepository;
    @Autowired
    private ChargeBeeSubscriptionIdRepository chargeBeeSubscriptionIdRepository;
    @Autowired
    private ChargeBeeUserRedirectedRepository chargeBeeUserRedirectedRepository;
    @Autowired
    private ChargeBeeSubscriptionService chargeBeeSubscriptionService;
    @Autowired
    private AccountService accountService;
    private DBconnection dbConnection;

    public ChargeBeeCustomerServiceImpl() {
        Environment.configure(funcclass.CB_SITE_NAME, funcclass.CB_API_KEY);
        dbConnection = DBconnection.GetDBconnection();
    }

    @Override
    public void saveChargeBeeId(final String chargeBeeId, final Long doctorId) {
        final ChargeBeeDoctorId chargeBeeDoctorId = new ChargeBeeDoctorId();
        chargeBeeDoctorId.setDoctorId(doctorId.intValue());
        chargeBeeDoctorId.setChargeBeeId(chargeBeeId);
        chargeBeeDoctorIdRepository.save(chargeBeeDoctorId);
    }

    @Override
    public void moveInCustomer(final Customer customer) throws Exception {
        synchronized (userCreationLock) {
            Long doctorId = dbConnection.getDocIdFromMailOrUserName(customer.email());
            if (doctorId == null || doctorId == -1) {
                doctorId = accountService.createDoctorFromCode(customer.firstName(), customer.lastName(), customer.email(), "bbb", "free_plan", "monthly", null, null, customer.optString("cf_password"), "");
            }
            if (doctorId == null || doctorId == -1) {
                throw new RuntimeException("Doctor was not created on cloud while CB signup");
            } else {
                DoctorService.setDoctorLegit(doctorId, true);
                saveChargeBeeId(customer.id(), doctorId);
            }
        }
    }

    @Override
    public PortalSession createPortalSession(final long doctorId, final String redirectUrl) throws Exception {
        final String cbId = chargeBeeId((int) doctorId);
        if (cbId != null) {
            final Result result = PortalSession.create().customerId(cbId).redirectUrl(redirectUrl).request();
            return result.portalSession();
        } else {
            return null;
        }
    }


    @Override
    public boolean isCbIdExists(final long doctorId) {
        return !chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByDoctorId((int) doctorId).isEmpty();
    }

    @Override
    public ChargeBeeRedirectDTO cbRedirect(final String hostedPageId, final String subscriptionId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final String cbId = cbIdFromHostedPageOrSubscription(hostedPageId, subscriptionId);
        if (cbId==null) {
            return new ChargeBeeRedirectDTO(false, funcclass.wpBaseDir3 + "/signup-error/");
        }
        final boolean cbIdIsNotInDb = chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByChargeBeeId(cbId).isEmpty();
        if (cbIdIsNotInDb) {

            synchronized (userCreationLock) {
                synchronized (ChargeBeeSubscriptionServiceImpl.subscriptionCreationLock) {
                    logger.info("Starting creating customer with cb id "+cbId+" on redirection");
                    final Customer customer = Customer.retrieve(cbId).request().customer();
                    Subscription subscription = null;
                    if (StringUtils.isNotBlank(hostedPageId)) {
                        final Result result = HostedPage.retrieve(hostedPageId).request();
                        final HostedPage hostedPage = result.hostedPage();
                        logger.info(hostedPage.jsonObj.toString());
                        if (hostedPage.content().subscription() != null) {
                            subscription = hostedPage.content().subscription();
                        }
                    }
                    if (StringUtils.isNotBlank(subscriptionId)) {                        ;
                        subscription = Subscription.retrieve(subscriptionId).request().subscription();
                    }
                    if (customer != null && subscription != null) {
                        moveInCustomer(customer);
                        chargeBeeSubscriptionService.createSubscription(subscription);
                        logger.info("Customer with cb id "+customer.id()+" is created on redirection");
                    } else {
                        return new ChargeBeeRedirectDTO(false, funcclass.wpBaseDir3 + "/signup-error/");
                    }
                }
            }
        }

        final List<ChargeBeeDoctorId> chargeBeeDoctorIds = chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByChargeBeeId(cbId);
        if (!chargeBeeDoctorIds.isEmpty()) {
            final ChargeBeeDoctorId chargeBeeDoctorId = chargeBeeDoctorIds.get(0);
            final BasicDoc docInfo = dbConnection.getDocInfo(new Long(chargeBeeDoctorId.getDoctorId()));
            if (docInfo.hasBillingPlan) {
                chargeBeeUserRedirectedRepository.save(new ChargeBeeUserRedirected(chargeBeeDoctorId.getChargeBeeId(), true));
                String subscriptionIdToCheck = subscriptionId;
                if (StringUtils.isBlank(subscriptionId)) {
                    if (StringUtils.isNotBlank(hostedPageId)) {
                        final Result result = HostedPage.retrieve(hostedPageId).request();
                        final HostedPage hostedPage = result.hostedPage();
                        logger.info(hostedPage.jsonObj.toString());
                        if (hostedPage.content().subscription() != null) {
                            subscriptionIdToCheck = hostedPage.content().subscription().id();
                        }
                    }
                }
                if (StringUtils.isNotBlank(subscriptionIdToCheck)) {
                    if (!chargeBeeSubscriptionIdRepository.existsBySubscriptionId(dbConnection.getDoctorSubscriptionId(docInfo.docnum))) {
                        logger.error("Subscription " + subscriptionIdToCheck + " is not yet registered. Doctor subscription id is " + dbConnection.getDoctorSubscriptionId(docInfo.docnum));
                        return new ChargeBeeRedirectDTO(false, funcclass.wpBaseDir3 + "/signup-error/");
                    }
                }
                return new ChargeBeeRedirectDTO(true, funcclass.baseUrl + "servlet/UserAccounts?action=login&password=" + docInfo.password + "&user=" + docInfo.user);
            }
        }
        return new ChargeBeeRedirectDTO(false, funcclass.wpBaseDir3 + "/signup-error/");
    }

    @Override
    public ChargeBeeRedirectDTO cbRedirectByDoctorId(final Long doctorId) throws Exception {
        final BasicDoc docInfo = dbConnection.getDocInfo(doctorId);
        return new ChargeBeeRedirectDTO(true, funcclass.baseUrl + "servlet/UserAccounts?action=login&password=" + docInfo.password + "&user=" + docInfo.user+"&hideCbRedirectPage=true");
    }


    private String cbIdFromHostedPageOrSubscription(final String hostedPageId, final String subscriptionId) throws Exception {
        if (StringUtils.isNotBlank(hostedPageId)) {
            final Result result = HostedPage.retrieve(hostedPageId).request();
            final HostedPage hostedPage = result.hostedPage();
            logger.info(hostedPage.jsonObj.toString());
            return hostedPage.content().customer().id();
        }
        if (StringUtils.isNotBlank(subscriptionId)) {
            final Result result = Subscription.retrieve(subscriptionId).request();
            final Subscription subscription = result.subscription();
            return subscription.customerId();
        }
        throw new RuntimeException("Cannot retrieve cb id. No search parameters specified");
    }

    @Override
    public boolean isCbUserRegistered(final String cbId, final HttpServletRequest request) {
        return !chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByChargeBeeId(cbId).isEmpty();
    }

    @Override
    public ChargeBeeRedirectDTO upgradeSubscriptionRedirect(final Integer doctorId, final HttpServletRequest request, final HttpServletResponse response) {
        final List<ChargeBeeDoctorId> chargeBeeDoctorIds = chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByDoctorId(doctorId);
        if (chargeBeeDoctorIds.isEmpty()) {
            return new ChargeBeeRedirectDTO(false, null);
        }
        final String redirectUrl = UriComponentsBuilder.fromUriString(funcclass.UPGRADE_SUBSCRIPTION_LINK)
            .queryParam("cbid", chargeBeeDoctorIds.get(0).getChargeBeeId())
            .queryParam("plan_name", dbConnection.getBillingPlanByDoctorId(doctorId.toString()).getName()).build().toUriString();
        return new ChargeBeeRedirectDTO(true, redirectUrl);
    }

    private String chargeBeeId(final int doctorId) {
        String cbId;
        final List<ChargeBeeDoctorId> chargeBeeDoctorIds = chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByDoctorId(doctorId);
        if (chargeBeeDoctorIds.isEmpty()) {
            throw new RuntimeException("");
        }
        cbId = chargeBeeDoctorIds.get(0).getChargeBeeId();
        return cbId;
    }

    @Override
    public boolean cbIdIsValid(final String cbId) throws Exception {
        try {
            final Customer customer = getCustomerFromChargeBee(cbId);
            return customer.id() != null && !customer.deleted();
        } catch (InvalidRequestException e) {
            return false;
        }
    }

    @Override
    public boolean customerHasPaymentMethod(final String cbId) throws Exception {
        try {
            final Customer customer = getCustomerFromChargeBee(cbId);
            return customer.paymentMethod()!=null;
        } catch (InvalidRequestException e) {
            return false;
        }
    }

    @Override
    public Customer getCustomerFromChargeBee(final String cbId) throws Exception {
        return Customer.retrieve(cbId).request().customer();
    }
}
