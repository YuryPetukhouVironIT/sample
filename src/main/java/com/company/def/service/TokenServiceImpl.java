package com.company.def.service;

import com.company.def.Base64;
import com.company.def.BasicDoc;
import com.company.def.DBconnection;
import com.company.def.funcclass;
import com.company.def.model.StlViewerToken;
import com.company.def.repository.StlViewerTokenRepository;
import com.company.def.repository.WpTokenRepository;
import com.company.def.service.cb.ChargeBeeCustomerService;
import com.company.def.util.string.StringUtility;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class TokenServiceImpl implements TokenService {

    private static final int MAX_INSERT_ATTEMPTS = 10000;

    @Autowired
    private WpTokenRepository wpTokenrepository;
    @Autowired
    private StlViewerTokenRepository stlViewerTokenRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ChargeBeeCustomerService chargeBeeCustomerService;

    @Override
    public String createWpToken() {
        String generatedString = null;
        int attempts = 0;
        boolean inserted = false;
        while (!inserted && attempts < MAX_INSERT_ATTEMPTS) {
            generatedString = StringUtility.MD5(Base64.encodeObject(RandomStringUtils.random(128)));
            inserted = DBconnection.GetDBconnection().insertWpToken(generatedString);
            attempts++;
        }
        return generatedString;
    }

    @Override
    public boolean checkWpToken(final String token) {
        return wpTokenrepository.existsWpTokenByToken(token);
    }

    @Override
    public void removeWpToken(final String token) {
        wpTokenrepository.deleteByToken(token);
    }

    @Override
    public String registerDoctor(final String redirectUrl) throws Exception {

        final URIBuilder uriBuilder = new URIBuilder(redirectUrl);
        final Map<String, String> parametersMap = new HashMap<>();
        String loginUrl = "";
        for (NameValuePair nameValuePair : uriBuilder.getQueryParams()) {
            parametersMap.put(nameValuePair.getName(), nameValuePair.getValue());
        }
        final String token = parametersMap.get("cus_token");
        if (!checkWpToken(token)) {
            return funcclass.wpBaseDir + "/error";
        }
        removeWpToken(token);
        final String cbId = parametersMap.get("cus_id");
        final String firstName = parametersMap.get("cus_fname");
        final String lastName = parametersMap.get("cus_lname");
        final String email = parametersMap.get("cus_email");
        final String password = parametersMap.get("cus_pw");
        final String country = parametersMap.get("cus_country");
        final String phone = parametersMap.get("cus_phone");
        final String zohoId = parametersMap.get("cus_zoho_id");
        final String billingPlanId = parametersMap.get("sub_plan_id");
        final String billingPlanType = "monthly";
        final String billingMethod = "bbb";

        final Long doctorId = accountService.createDoctorFromCode(firstName, lastName, email, billingMethod, billingPlanId, billingPlanType, phone, country, password, zohoId);
        if (doctorId != null && doctorId != -1) {
            chargeBeeCustomerService.saveChargeBeeId(cbId, doctorId);
            final BasicDoc docInfo = DBconnection.GetDBconnection().getDocInfo(doctorId);

            loginUrl = String.format(funcclass.wpBaseDir + "/servlet/UserAccounts?action=login&password=%s&user=%s", password, docInfo.user);
        }

        return loginUrl;
    }

    @Override
    public String createViewerToken() {
        final StlViewerToken token = new StlViewerToken();
        int attempts = 0;
        boolean inserted = false;
        while (!inserted && attempts < MAX_INSERT_ATTEMPTS) {
            final String generatedString = StringUtility.MD5(Base64.encodeObject(RandomStringUtils.random(128)));
            if (!stlViewerTokenRepository.existsByToken(generatedString)) {
                token.setToken(generatedString);
                final Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, 1);
                token.setExpirationDateTime(cal.getTime());
                inserted = stlViewerTokenRepository.save(token) != null;
            } else {
                attempts++;
            }
        }
        if (attempts >= MAX_INSERT_ATTEMPTS) {
            throw new RuntimeException("Failed to create STL viewer authentication token");
        }
        return token.getToken();
    }

    @Override
    public boolean checkViewerToken(final String token) {
        return stlViewerTokenRepository.existsByToken(token);
    }
}
