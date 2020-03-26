package com.cephx.def.service;

import com.cephx.def.BasicDoc;
import com.cephx.def.DBconnection;
import com.cephx.def.SlackClient;
import com.cephx.def.ZohoCrmClient;
import com.cephx.def.enums.BillingMethod;
import com.cephx.def.funcclass;
import com.cephx.def.model.DoctorLogin;
import com.cephx.def.model.billing.BillingPlan;
import com.cephx.def.model.chargebee.ChargeBeeDoctorId;
import com.cephx.def.pdf;
import com.cephx.def.repository.ChargeBeeDoctorIdRepository;
import com.cephx.def.repository.billing.BillingPlanRepository;
import com.cephx.def.service.billing.BillingPlanService;
import com.cephx.def.service.cb.ChargeBeeCustomerService;
import com.cephx.def.service.cb.ChargeBeeSubscriptionService;
import com.cephx.def.service.db.AnalysisFilterService;
import com.cephx.def.service.db.DoctorLoginService;
import com.cephx.def.service.db.DoctorService;
import com.cephx.def.service.db.PartnerService;
import com.cephx.def.service.db.PatientImageService;
import com.cephx.def.service.db.PatientService;
import com.cephx.def.servlets.accounts.PasswordHash;
import com.cephx.def.servlets.accounts.UserAccounts;
import com.cephx.def.servlets.accounts.UserAccountsConsts;
import com.cephx.def.servlets.admin.Partner;
import com.cephx.def.servlets.util.BrowserInfo;
import com.cephx.def.servlets.util.CephxSession;
import com.cephx.def.servlets.util.SessionUtility;
import com.cephx.def.struct.struct.TxAttach;
import com.cephx.def.system;
import com.cephx.def.tools.ExcelBuilder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

@Service
public class AccountService {

    private static final Logger logger = LogManager.getLogger(AccountService.class);
    private static final long CT_DENT_MASTER_ID = 320L;
    public static final String ACTION_PARAMETER_NAME = "action";
    public static final String FIRST_NAME_PARAMETER_NAME = "fname";
    public static final String LAST_NAME_PARAMETER_NAME = "lname";
    public static final String EMAIL_PARAMETER_NAME = "email";
    public static final String PASSWORD_PARAMETER_NAME = "password";
    public static final String USERTYPE_PARAMETER_NAME = "utype";
    public static final String IS_ORTO2_PARAMETER_NAME = "isOrto2";
    public static final String API_KEY_PARAMETER_NAME = "api_key";
    public static final String USER_PARAMETER_NAME = "user";
    public static final String DOCTOR_ID_PARAMETER_NAME = "doctor_id";
    public static final String PAYMENT_METHOD_PARAMETER_NAME = "billing_method";
    public static final String MASTER_ID_PARAMETER_NAME = "master_id";
    public static final String PHONE_PARAMETER_NAME = "phone";
    public static final String COUNTRY_PARAMETER_NAME = "country";
    public static final String ZOHO_ID_PARAMETER_NAME = "zohoId";
    public static final String BILLING_PLAN_ID_PARAMETER_NAME = "billingPlanId";
    public static final String BILLING_PLAN_TYPE_PARAMETER_NAME = "billingPlanType";

    private DBconnection db = DBconnection.GetDBconnection();

    @Autowired
    private PartnerService partnerService;
    @Autowired
    private DoctorLoginService doctorLoginService;
    @Autowired
    private BillingPlanService billingPlanService;
    @Autowired
    private PatientImageService patientImageService;
    @Autowired
    private BillingPlanRepository billingPlanRepository;
    @Autowired
    private ChargeBeeDoctorIdRepository chargeBeeDoctorIdRepository;
    @Autowired
    private ChargeBeeCustomerService chargeBeeCustomerService;
    @Autowired
    private ChargeBeeSubscriptionService chargeBeeSubscriptionService;


    public void registerNewDoctor(HttpServletRequest vReq, HttpServletResponse vRes) {
        try {
            HttpSession session = vReq.getSession(true);
            //create json object
            JSONObject jsonError = new JSONObject();
            PrintWriter out = vRes.getWriter();
            vRes.setContentType("application/json");

            String sharedPatientNumber = vReq.getParameter("sharedPatientNumber");
            if (!StringUtils.isEmpty(sharedPatientNumber)) {
                session.setAttribute("sharedPatientNumber", sharedPatientNumber);
            }

            //initializing redirect_url with empty value
            jsonError.put("redirect_url", "");

            String instant = "";
            if (vReq.getParameter("instant") != null) {
                if (vReq.getParameter("instant").equals("true")) {
                    session.setAttribute("instant", "true");
                    instant = "?instant=true";
                } else {
                    instant = "";
                }
            }


            BasicDoc dc = createNewDoctorInfo(vReq);

            DBconnection cn = UserAccounts.getDBConnection();

            String apiKey = vReq.getParameter("api_key");
            Partner partner = null;
            if (apiKey!=null) {
                partner = PartnerService.getPartnerByAPiKey(apiKey);
                if (partner == null) {
                    logger.warn("Invalid api key {}", apiKey);
                    vRes.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    vRes.setHeader("Message", "Invalid api key");
                    return;
                }
            }


            if (dc.extDoctorId != null && !dc.extDoctorId.isEmpty() && cn.isExtDoctorIdExist(dc.extDoctorId)) {
                jsonError.put("redirect_url", UserAccountsConsts.PG_ERROR_PAGE);
                jsonError.put("user_already_exists", "true");
                jsonError.put("cephxId", -1);
                out.print(jsonError.toString());
                out.close();
                return;
            }

            if (vReq.getParameter("doctor_id") != null) {
                dc.extDoctorId = vReq.getParameter("doctor_id");
            }
            dc.setLanguage("en");
            if (partner != null) {
                if (partner.getName().equals("CT-Dent")) {
                    dc.country = "IL";
                }
                if (partner.isAutoActiveDoctor()) {
                    dc.isAlgoCeph = true;
                    dc.isLegit = true;
                    dc.allowedToTrace = true;
                    dc.billingMethod = null;
                    dc.partnerId = partner.getId();
                }
                if (partner.getName().equals("CT-Dent")) {
                    dc.country = "IL";
                    dc.isLegit = true;
                    dc.allowedToTrace = true;
                    dc.billingMethod = BillingMethod.CHARGE_LINK;

                }
                String languageCode = vReq.getParameter("lang");
                if (!StringUtils.isEmpty(languageCode)
                    && LanguageService.isValidLanguageCode(languageCode)
                    && partner.getId() == funcclass.HOME_SITE_PARTNER_ID) {
                    dc.setLanguage(languageCode);
                } else {
                    dc.setLanguage("en");
                }

                if (dc.freeAnalyses == null) {
                    dc.freeAnalyses = partner.getFreeAnalyses();
                }
                dc.freeMonths = partner.getFreeMonths();
                dc.prepaidAnalyses = partner.getPrepaidAnalyses();
            }
            if (vReq.getParameter("billing_method") != null) {
                dc.billingMethod = vReq.getParameter("billing_method");
            }
            try {

                // ido: hash + salt pass before insert to DB. save the hash in doctors table
                dc.password = PasswordHash.createHash(dc.password);
                if (!StringUtils.isBlank(dc.extDoctorId)) {
                    dc.docnum = cn.AddDocToDbWithId(dc);
                } else {
                    dc.docnum = cn.AddDocToDb(dc);
                }
                if (dc.docnum > 0) {
                    Long masterId = null;
                    if (StringUtils.isNotBlank(vReq.getParameter("master_id"))) {
                        masterId = Long.parseLong(vReq.getParameter("master_id"));
                    } else {
                        if (partner != null) {
                            masterId = partner.getMasterId();
                        }
                    }
                    if (masterId != null) {
                        cn.insertNewMasterId(dc.docnum, masterId);
                    }
                }
            } catch (Exception e) {
                logger.error("Something wrong during saving doctor", e);
                //initializing redirect_url with UserAccountsConsts.PG_ERROR_PAGE
                jsonError.put("redirect_url", UserAccountsConsts.PG_ERROR_PAGE);
                //initializing user_already_exists with false value
                jsonError.put("user_already_exists", "false");
                jsonError.put("cephxId", -1);
                out.print(jsonError.toString());
                out.close();
                return;
            }
            //if user already exists
            if (dc.docnum == -1) {
                PatientService.updateSharedWithDoctor(dc.email, dc.docnum, dc.getDocFullName());
                session.setAttribute("DocInfo", dc);
                //assigning values to json object
                jsonError.put("redirect_url", (dc.isOrto2) ? UserAccountsConsts.PG_REGISTRATION_ORTO2 + instant : UserAccountsConsts.PG_REGISTRATION + instant);
                jsonError.put("user_already_exists", "true");
                jsonError.put("cephxId", dc.docnum);
                out.print(jsonError.toString());
                out.close();

                return;

                //return (dc.isOrto2) ? UserAccountsConsts.PG_REGISTRATION_ORTO2 + instant : UserAccountsConsts.PG_REGISTRATION + instant;
            }
            if (!isQaOrLiveServer(apiKey) && funcclass.isProdEnvironment()) {
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("Country", dc.country);
                parameters.put("City", dc.city);
                parameters.put("State", dc.state);
                parameters.put("Email", dc.email);
                parameters.put("First Name", dc.name);
                parameters.put("Last Name", dc.lastname);
                parameters.put("Lead Source", "Api");
                parameters.put("Partner", partner == null ? "" : partner.getName());
                parameters.put("Phone", dc.phoneNumber);
                parameters.put("cephX_ID", dc.docnum + "");
                parameters.put("wfTrigger", true);
                parameters.put(ZohoCrmClient.TOTAL_CEPHS_UPLOADED, "0");
//                parameters.put("Environment", funcclass.getEnvironment());
                ZohoCrmClient.addLeadRecord(parameters);
                db.insertNotUpdatedDoctor(dc.docnum);
            }

            sendNotificationToSlack(dc, vReq.getRemoteAddr());

            try {// inser default doctor preferences...
                cn.insertDefaultPreferencesForNewDoctor(dc.docnum);
                if (funcclass.isProdEnvironment() || funcclass.isQaEnvironment()) {
                    if (partner!=null) {
                        copyPartnerFiltersToDoctor(partner, dc.docnum);
                    }
                }
                DoctorService.insertDemoPatientForDoctor(dc.docnum, patientImageService.getPatientImages(funcclass.DEMO_PAT_ID));
                logger.info("in UserAccountsService.registerNewDoctor done with setDoctorsFirstPat");
                cn.newDocQuickHelp(dc.docnum);
                logger.info("in UserAccountsService.registerNewDoctor done with newDocQuickHelp");
                cn.newDoctorHomePage(dc.docnum);
                logger.info("in UserAccountsService.registerNewDoctor done with setDoctorsFirstPat");
            } catch (Exception e) {
                logger.error("Unable to set default preferences for new doctor", e);
            }
            // NewDoc is used mean while to open help popup after new user is created
            if (!dc.isOrto2) {
                session.setAttribute("IamNewDoctor", "NewDoc");
            }
            //	        assigning values to json object
            if (StringUtils.isNotBlank(vReq.getParameter("billingPlanId")) && StringUtils.isNotBlank(vReq.getParameter("billingPlanType"))) {
                cn.addDoctorBillingPlan(dc.docnum, Long.parseLong(vReq.getParameter("billingPlanId")), vReq.getParameter("billingPlanType"));
            }
            jsonError.put("redirect_url", doLogin(vReq, dc.user, dc.password, true));
            DoctorService.deleteLogins(dc.docnum);
            jsonError.put("cephxId", dc.docnum);
            jsonError.put("cephxId", dc.docnum);
            jsonError.put("user_already_exists", "false");
            logger.info("Registering new doctor: jsonResponse = {}", jsonError);
            out.print(jsonError.toString());
            out.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void sendNotificationToSlack(BasicDoc dc, String ipAddress) {
        if (!funcclass.getEnvironment().toLowerCase().equals("dev")) {
            StringBuilder slackMessage = new StringBuilder();
            slackMessage.append("User Registered:\n");
            slackMessage.append("No:").append(dc.docnum).append("\n");
            slackMessage.append("Name:").append(dc.getDocFullName()).append("\n");
            slackMessage.append("Email:").append(dc.email).append("\n");
            if (dc.partner != null) {
                slackMessage.append("Partner:").append(dc.partner.getName()).append("\n");
            }

            if (dc.country != null) {
                slackMessage.append("Country:").append(dc.country).append("\n");
            }

            slackMessage.append("Monthly cost:").append(dc.monthlyCost).append("\n");
            slackMessage.append("Cost per case:").append(dc.costPerCase).append("\n");
            slackMessage.append("IP:").append(ipAddress).append("\n");
            slackMessage.append("Env:").append(funcclass.getEnvironment()).append("\n");

            SlackClient.sendMessageToUrl(slackMessage.toString(), SlackClient.urlForNewDoc);
        }
    }

    private static void sendLoginFailsToSlack(String user, String ipAddress, String password) {

        if (!funcclass.getEnvironment().toLowerCase().equals("dev")) {
            StringBuilder slackMessage = new StringBuilder();
            slackMessage.append("User Login Fail:\n");
            slackMessage.append("Name:").append(user).append("\n");
            slackMessage.append("Password:").append(password).append("\n");
            slackMessage.append("IP:").append(ipAddress).append("\n");
            slackMessage.append("Env:").append(funcclass.getEnvironment()).append("\n");
            SlackClient.sendMessageToUrl(slackMessage.toString(), SlackClient.urlSupportChannel);
        }
    }

    /**
     * Copying filters for new registered doctor from master account of his partner
     *
     * @param partner      Partner of registered doctor
     * @param doctorNumber Doctor's number
     */
    private void copyPartnerFiltersToDoctor(Partner partner, long doctorNumber) {
        // Copying specially for this partner
        if (partner.getName().equals("CT-Dent")) {
            try {
                logger.info("Copying analysis filters from doc {} to doc {}", CT_DENT_MASTER_ID, doctorNumber);
                AnalysisFilterService.copyAnalysisFiltersToDoctor(CT_DENT_MASTER_ID, doctorNumber);
            } catch (Exception e) {
                logger.error("Error during copying analysis filters", e);
            }
        }
    }

    public String registerAndLogin(HttpServletRequest vReq, HttpServletResponse vRes) {
        try {
            HttpSession session = vReq.getSession(true);

            String sharedPatientNumber = vReq.getParameter("sharedPatientNumber");
            if (!StringUtils.isEmpty(sharedPatientNumber)) {
                session.setAttribute("sharedPatientNumber", sharedPatientNumber);
            }

            String instant = "";
            if (vReq.getParameter("instant") != null) {
                if (vReq.getParameter("instant").equals("true")) {
                    session.setAttribute("instant", "true");
                    instant = "?instant=true";
                } else {
                    instant = "";
                }
            }


            BasicDoc dc = createNewDoctorInfo(vReq);

            DBconnection cn = UserAccounts.getDBConnection();

            if (dc.extDoctorId != null && !dc.extDoctorId.isEmpty() && cn.isExtDoctorIdExist(dc.extDoctorId)) {
                return UserAccountsConsts.PG_ERROR_PAGE;
            }

            dc.isLegit = true;
            dc.allowedToTrace = true;
            dc.isAlgoCeph = true;
            dc.billingMethod = BillingMethod.SHARE_DOC;
            dc.freeAnalyses = 1;
            dc.setLanguage("en");
            dc.monthlyCost = 0;
            try {

                // ido: hash + salt pass before insert to DB. save the hash in doctors table
                dc.password = PasswordHash.createHash(dc.password);

                dc.docnum = cn.AddDocToDb(dc);

            } catch (Exception e) {
                logger.error("Can't add doctor to database", e);
                return UserAccountsConsts.PG_ERROR_PAGE;
            }
            //if user already exists
            if (dc.docnum == -1) {
                return UserAccountsConsts.PG_ERROR_PAGE;
            }

            PatientService.updateSharedWithDoctor(dc.email, dc.docnum, dc.getDocFullName());

            sendNotificationToSlack(dc, vReq.getRemoteAddr());

            try {
                cn.insertDefaultPreferencesForNewDoctor(dc.docnum);
                DoctorService.insertDemoPatientForDoctor(dc.docnum, patientImageService.getPatientImages(funcclass.DEMO_PAT_ID));
                cn.newDocQuickHelp(dc.docnum);
                cn.newDoctorHomePage(dc.docnum);
            } catch (Exception e) {
                logger.error("Unable to set default preferences for doctor " + dc.docnum, e);
            }
            // sendMailNewDoctor(dc);
            // NewDoc is used mean while to open help popup after new user is created
            if (!dc.isOrto2) {
                session.setAttribute("IamNewDoctor", "NewDoc");
            }
            return doLogin(vReq, dc.user, dc.password, true);

        } catch (Exception e) {
            logger.error("Error in registerAndLogin action", e);
        }
        return UserAccountsConsts.PG_ERROR_PAGE;
    }

    private boolean isQaOrLiveServer(String apiKey) {
        return apiKey==null || apiKey.equals("cephxlivekey") || apiKey.equals("qa_key_wev6ju");
    }

    private String doLogin(HttpServletRequest vReq, String vUsr, String vPass, boolean invalidateSession) throws Exception {

        HttpSession session = vReq.getSession();
        final String sharedPatientNumber = (String) session.getAttribute("sharedPatientNumber");

        int loginAttempt;
        if (session.getAttribute("loginCount") == null) {
            session.setAttribute("loginCount", 0);
            loginAttempt = 0;
        } else {
            loginAttempt = (Integer) session.getAttribute("loginCount");
        }

        logger.info("Login count for user {} is {}", vUsr, loginAttempt);

        //this is 3 attempt counting from 0,1,2
        if (loginAttempt >= 2) {
            final long lastAccessedTime = session.getLastAccessedTime();
            final Date date = new Date();
            final long currentTime = date.getTime();
            final long timeDiff = currentTime - lastAccessedTime;
            // 20 minutes in milliseconds
            if (timeDiff >= 1200000) {
                //invalidate user session, so they can try again
                session.invalidate();
            } else {
                // Error message
                // session.setAttribute("message","You have exceeded the 3 failed login attempt. Please try loggin in in 20 minutes, or call our customer service center at 1-800 555-1212.");
            }

        } else {
            loginAttempt++;
            final int allowLogin = 3 - loginAttempt;
            //  session.setAttribute("message","loginAttempt= "+loginAttempt+". Invalid username or password. You have "+allowLogin+" attempts remaining. Please try again! <br>Not a registered cusomer? Please <a href=\"register.jsp\">register</a>!");
        }
        session.setAttribute("loginCount", loginAttempt);

        //*******************************end
        clearSession(session);
        try {
            if (!BrowserInfo.isApplicationBrowser(vReq)) {
                session.setAttribute(UserAccountsConsts.PRM_ERR_MSG, UserAccountsConsts.TXT_BAD_BROWSER);
                logger.warn("Incompatible browser login attempt.");
                return UserAccountsConsts.PG_LOGIN;
            }
        } catch (Exception e) {
            session.setAttribute(UserAccountsConsts.PRM_ERR_MSG, UserAccountsConsts.TXT_BAD_BROWSER);
            logger.warn("Incompatible browser login attempt.");
            return UserAccountsConsts.PG_LOGIN;
        }

        final String goodHash = PasswordHash.getPassHashFromDb(vUsr);
        logger.info("Password received is {}", vPass);
        logger.info("Right hash of password is {}", goodHash);
        boolean valid = true;

        boolean isShare = DBconnection.GetDBconnection().IsShare(vUsr, vPass);
        try {
            if ((!isShare && !PasswordHash.validatePassword(vPass, goodHash))) {
                valid = false;
                if (PasswordHash.checkEquals(vPass, goodHash)) {
                    valid = true;
                    logger.info("Login from admin panel");
                } else {
                    sendLoginFailsToSlack(vUsr, vReq.getRemoteAddr(), vPass);
                    logger.info("Failed login for user {}", vUsr);
                    return UserAccountsConsts.PG_ERROR_PAGE;
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
        logger.info("password is valid? " + valid);

        //int logintype = getLoginType(vUsr, vPass); line changed due to password hashing.
        int logintype = 0;
        if (isShare) {
            logintype = getLoginType(vUsr, vPass);
        } else {
            logintype = getLoginType(vUsr, goodHash);
        }


        logger.info("in UserAccountsService.doLogin loginType is: " + logintype);
        if (isHacker(vUsr, vPass) || logintype == funcclass.ERROR) {
            logger.info("Hack Attepmt - Please note!!!!");
            session.setAttribute(UserAccountsConsts.PRM_ERR_MSG, UserAccountsConsts.TXT_BAD_LOG_INFO);
            return UserAccountsConsts.PG_ERROR_PAGE;
        }

        //*** invalidate current session and get a new session
        try {
            if (invalidateSession) {
                session.invalidate();
            }
        } catch (Exception ex) {
        }
        session = vReq.getSession(true);
        session.setAttribute("currentLanguageCode", DoctorService.getDoctorLanguage(vUsr));
        final String ip = getIp(vReq);
        final long doctorId = DoctorService.getDoctorNumberFromUser(vUsr);
        DBconnection.GetDBconnection().addSessionInfo(ip, vReq.getHeader("User-Agent"), doctorId);
        if (vReq.getParameter("hideCbRedirectPage") == null) {
            setSubscriptionRedirectSessionAttribute(session, doctorId);
        } else {
            session.setAttribute("redirectToBillingPlanUpgradePage", false);
        }
        //****
        return redirectByLoginType(vReq, session, logintype, vUsr, vPass, sharedPatientNumber);
    }

    private String doLoginLite(HttpServletRequest vReq, String vUsr, String vPass) throws Exception {

        HttpSession session = vReq.getSession(true);
        clearSession(session);

        int logintype = getLoginType(vUsr, vPass);

        if (isHacker(vUsr, vPass) || logintype == funcclass.ERROR) {
            return UserAccountsConsts.LOGIN_LITE_PAGE + "?err=invalid_login&company=" + vReq.getParameter("company");
        }
        BasicDoc doc = null;
        long dn = 0;
        try {
            dn = DoctorService.getDoctorNumberFromUser(vUsr);
            doc = UserAccounts.getDBConnection().getDocInfo(dn);
        } catch (Exception e) {
            system.printStackTrace(e);
            return UserAccountsConsts.LOGIN_LITE_PAGE + "?err=invalid_login&company=" + vReq.getParameter("company");
        }

        String ret = UserAccountsConsts.ACCOUNT_LITE_PAGE;
        session.setAttribute("lite_docobj", doc);
        String company = vReq.getParameter("company");
        if (company != null && !company.trim().equals("")) {
            ret += "?company=" + company;
        }
        final String ip = getIp(vReq);
        final long doctorId = DoctorService.getDoctorNumberFromUser(vUsr);
        DBconnection.GetDBconnection().addSessionInfo(ip, vReq.getHeader("User-Agent"), doctorId);
        if (vReq.getParameter("hideCbRedirectPage") == null) {
            setSubscriptionRedirectSessionAttribute(session, doctorId);
        } else {
            session.setAttribute("redirectToBillingPlanUpgradePage", false);
        }
        return ret;
    }

    private String doLoginLiteHeb(HttpServletRequest vReq, String vUsr, String vPass) throws Exception {

        HttpSession session = vReq.getSession(true);
        clearSession(session);

        int logintype = getLoginType(vUsr, vPass);

        if (isHacker(vUsr, vPass) || logintype == funcclass.ERROR) {
            return UserAccountsConsts.LOGIN_LITE_PAGE_HEB + "?err=invalid_login&company=" + vReq.getParameter("company");
        }
        BasicDoc doc = null;
        long dn = 0;
        try {
            dn = DoctorService.getDoctorNumberFromUser(vUsr);
            doc = UserAccounts.getDBConnection().getDocInfo(dn);
        } catch (Exception e) {
            system.printStackTrace(e);
            return UserAccountsConsts.LOGIN_LITE_PAGE_HEB + "?err=invalid_login&company=" + vReq.getParameter("company");
        }

        String ret = UserAccountsConsts.ACCOUNT_LITE_PAGE_HEB;
        session.setAttribute("lite_docobj", doc);
        String company = vReq.getParameter("company");
        if (company != null && !company.trim().equals("")) {
            ret += "?company=" + company;
        }
        final String ip = getIp(vReq);
        final long doctorId = DoctorService.getDoctorNumberFromUser(vUsr);
        DBconnection.GetDBconnection().addSessionInfo(ip, vReq.getHeader("User-Agent"), doctorId);
        if (vReq.getParameter("hideCbRedirectPage") == null) {
            setSubscriptionRedirectSessionAttribute(session, doctorId);
        } else {
            session.setAttribute("redirectToBillingPlanUpgradePage", false);
        }
        return ret;
    }

    private void setSubscriptionRedirectSessionAttribute(final HttpSession session, final long doctorId) throws Exception {
        final boolean redirectToBillingPlanUpgradePage = getRedirectToBillingPlanUpgradePage((int) doctorId);
        session.setAttribute("redirectToBillingPlanUpgradePage", redirectToBillingPlanUpgradePage);
        if (redirectToBillingPlanUpgradePage) {
            if (!"free_plan".equals(DBconnection.GetDBconnection().getBillingPlanByDoctorId(String.valueOf(doctorId)).getName())) {
                DBconnection.GetDBconnection().addDoctorBillingPlan(doctorId, "free_plan", "monthly");
            }
        }
    }

    private String getIp(final HttpServletRequest vReq) {
        final String xForwardedForHeader = vReq.getHeader("X-Forwarded-For");
        return xForwardedForHeader == null ? vReq.getRemoteAddr() : new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
    }

    public String emailLogin(HttpServletRequest vReq) {
        HttpSession session = vReq.getSession(true);
        session.setAttribute(UserAccountsConsts.SES_DIRECT_PATIENT, vReq.getParameter("patnum"));
        session.setAttribute(UserAccountsConsts.SES_START_WITH_PAGE, UserAccountsConsts.PG_LOGIN);
        //session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_APP_INDEX);
        //return 	UserAccountsConsts.PG_APP_REDIRECT;
        return UserAccountsConsts.PG_APP_INDEX;
    }

    public String doctorExit(HttpServletRequest vReq) {
        HttpSession session = vReq.getSession(true);
        String SPage = vReq.getParameter(UserAccountsConsts.SES_START_WITH_PAGE);
        if (SPage == null) {
            SPage = UserAccountsConsts.PG_DEF_START_PAGE;
        }
        session.setAttribute(UserAccountsConsts.SES_START_WITH_PAGE, SPage);
        //session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_APP_INDEX);
        //return 	UserAccountsConsts.PG_APP_REDIRECT;
        return UserAccountsConsts.PG_APP_INDEX;
    }

    public String patLogin(HttpServletRequest vReq) throws Exception {
        String url = doLogin(vReq, true);
        HttpSession session = vReq.getSession();
        String goToUrl = (String) session.getAttribute(UserAccountsConsts.PRM_LOCATION);
        session.removeAttribute(UserAccountsConsts.PRM_LOCATION);
        return goToUrl;
    }

    public String sideDoorLogin(HttpServletRequest vReq) throws Exception {
        String url = doLogin(vReq, true);
        //HttpSession session = vReq.getSession();
		/*
        //String location = (String)session.getAttribute(UserAccountsConsts.PRM_LOCATION);
        System.out.println("**** Url is *** "+url);
        if(url.equals(UserAccountsConsts.PG_PARENT_REDIRECT))  ///location.equals(UserAccountsConsts.PG_LOGIN)) // if login faled ...
        {
            System.out.println("REMOVING REGULAR PARAMS ... ");
            session.removeAttribute(UserAccountsConsts.PRM_LOCATION);
            session.setAttribute(UserAccountsConsts.SES_START_WITH_PAGE,UserAccountsConsts.PG_LOGIN);
            url = UserAccountsConsts.PG_APP_INDEX;
            System.out.println("Now URL is ");
        }
		 */
        return url;
    }

    public String doLogin(HttpServletRequest vReq, boolean invalidateSession) throws Exception {
        HttpSession session = vReq.getSession(true);
        String sharedPatientNumber = vReq.getParameter("sharedPatientNumber");
        if (!StringUtils.isEmpty(sharedPatientNumber)) {
            session.setAttribute("sharedPatientNumber", sharedPatientNumber);
        }

        String usr = vReq.getParameter(UserAccountsConsts.PRM_USER);
        String pas = vReq.getParameter(UserAccountsConsts.PRM_PASSWORD);
        logger.info("doLogin   User: " + usr + " Password: " + pas);

        if (usr == null || pas == null) {

            return UserAccountsConsts.PG_ERROR_PAGE;
        }
        return doLogin(vReq, usr, pas, invalidateSession);
    }

    public String doLoginLite(HttpServletRequest vReq) throws Exception {

        HttpSession session = vReq.getSession();
        String usr = vReq.getParameter(UserAccountsConsts.PRM_USER);
        String pas = vReq.getParameter(UserAccountsConsts.PRM_PASSWORD);
        System.out.println("LOGIN eceph (cephX lite) USER " + usr + "  PASS " + pas);
        if (usr == null || pas == null) {


            //session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_ERROR_PAGE);
            //return UserAccountsConsts.PG_PARENT_REDIRECT;
            return UserAccountsConsts.PG_ERROR_PAGE;
        }
        //session.setAttribute(UserAccountsConsts.PRM_PATNUM, vReq.getParameter(UserAccountsConsts.PRM_PATNUM));
        return doLoginLite(vReq, usr, pas);
    }

    public String doLoginLiteHeb(HttpServletRequest vReq) throws Exception {

        HttpSession session = vReq.getSession();
        String usr = vReq.getParameter(UserAccountsConsts.PRM_USER);
        String pas = vReq.getParameter(UserAccountsConsts.PRM_PASSWORD);
        System.out.println("LOGIN eceph Heb (cephX lite) USER " + usr + "  PASS " + pas);
        if (usr == null || pas == null) {


            //session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_ERROR_PAGE);
            //return UserAccountsConsts.PG_PARENT_REDIRECT;
            return UserAccountsConsts.PG_ERROR_PAGE;
        }
        //session.setAttribute(UserAccountsConsts.PRM_PATNUM, vReq.getParameter(UserAccountsConsts.PRM_PATNUM));
        return doLoginLiteHeb(vReq, usr, pas);
    }

    public void checkCredentials(HttpServletRequest request, HttpServletResponse response) {
        String user = request.getParameter(UserAccountsConsts.PRM_USER);
        String password = request.getParameter(UserAccountsConsts.PRM_PASSWORD);
        if (user == null || password == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            try {
                String goodHash = PasswordHash.getPassHashFromDb(user);
                boolean isValid = PasswordHash.validatePassword(password, goodHash);
                response.setStatus(isValid ? HttpStatus.SC_OK : HttpStatus.SC_UNAUTHORIZED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Boolean getRedirectToBillingPlanUpgradePage(final int doctorId) throws Exception {
        final List<ChargeBeeDoctorId> chargeBeeDoctorIds = chargeBeeDoctorIdRepository.findChargeBeeDoctorIdsByDoctorId(doctorId);
        boolean redirectToBillingPlanUpgradePage = false;
        if (!chargeBeeDoctorIds.isEmpty()) {
            final String cbId = chargeBeeDoctorIds.get(0).getChargeBeeId();
            final BillingPlan currentBillingPlan =db.getBillingPlanByDoctorId(String.valueOf(doctorId));
            if (chargeBeeCustomerService.cbIdIsValid(cbId) && currentBillingPlan!=null && !currentBillingPlan.isFree()) {
                redirectToBillingPlanUpgradePage = !chargeBeeSubscriptionService.customerHasActiveSubscription(cbId);
            }
        }
        return redirectToBillingPlanUpgradePage;
    }

    private int getLoginType(String vUser, String vPass) {
        int type = UserAccounts.getDBConnection().getLoginType(vUser, vPass);

        return type;
    }

    public String redirectByLoginType(HttpServletRequest request, HttpSession vS, int vLoginType, String vUsr, String vPass, String sharedPatientNumber) {
        logger.info("Parameters of login : loginType = {}, user = {}, password = {}", vLoginType, vUsr, vPass);
        SessionUtility.cleanSession(vS);
        CephxSession cs = new CephxSession(); //setDocByType(vLoginType,vUsr,vPass);
        cs.put(CephxSession.LOGIN_TYPE, "" + vLoginType);
        cs.put(CephxSession.USER, "" + vUsr);
        cs.put(CephxSession.PASS, "" + vPass);

        vS.setAttribute(cs.OBJ_NAME, cs);

        //       // create a temporary directory
        //       String tempDirPath = SessionUtility.createDirectory();
        //       //check if dir is valid
        //       if((tempDirPath!=null)&&(tempDirPath!=""))
        //       {
        //    	   //assign value to session parameter
        //    	   cs.SESSION_TEMP_DIR_PATH = tempDirPath;
        //       }

        if (vLoginType == funcclass.DOCTOR || vLoginType == funcclass.OPERATOR || vLoginType == funcclass.GP || vLoginType == funcclass.STUDENT || vLoginType == funcclass.COMPANY) {
            long doctorNumber;
            try {
                doctorNumber = DoctorService.getDoctorNumberFromUser(vUsr);
                logger.info("Doctor {} is logining in system", doctorNumber);
            } catch (Exception e) {
                return UserAccountsConsts.PG_ERROR_PAGE;
            }
            boolean isFirstLogin = DoctorService.isFirstLogin(doctorNumber);
            vS.setAttribute("isFirstLogin", isFirstLogin);
            UserAccounts.getDBConnection().incCounter("login", doctorNumber);
            cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL);
            cs.put(cs.DOC_PREF, UserAccounts.getDBConnection().getDocPreferences(doctorNumber));
            vS.setAttribute("num", String.valueOf(doctorNumber));
            String instant = (String) vS.getAttribute("instant");
            if (instant != null && instant.equals("true")) {
                instant = "&instant=true";
            } else {
                instant = "";
            }

            vS.removeAttribute("instant");

            DoctorLogin doctorLogin = new DoctorLogin();
            doctorLogin.setDoctorId(doctorNumber);
            doctorLogin.setCephxVersion(funcclass.versionNumber);
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (StringUtils.isEmpty(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            } else {
                int commaIndex = ipAddress.indexOf(",");
                if (commaIndex > 0) {
                    ipAddress = ipAddress.substring(0, commaIndex);
                }
            }
            doctorLogin.setIp(ipAddress);
            doctorLoginService.insertDoctorLogin(doctorLogin);

            int loginCountForLastVersion = doctorLoginService.getLoginCountForLastVersion(doctorNumber);
            vS.setAttribute("lastVersionLoginCount", loginCountForLastVersion);


            String sharedPatientParameter = "";
            if (!StringUtils.isEmpty(sharedPatientNumber)) {
                logger.info("Doctor will be redirected to shared patient {}", sharedPatientNumber);
                sharedPatientParameter = (StringUtils.isEmpty(instant) ? "?" : "&") + "sharedPatientNumber=" + sharedPatientNumber;
            }


            if (funcclass.isProdEnvironment()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Map<String, String> parameters = new HashMap<>();
                parameters.put(ZohoCrmClient.LAST_CLOUD_LOGIN, dateFormat.format(new Date()));
                parameters.put(ZohoCrmClient.TOTAL_LOGIN_COUNT, String.valueOf(doctorLoginService.getTotalCountLogins(doctorNumber)));
                ZohoCrmClient.updateAccountsRecord(parameters, doctorNumber);
            }

            Enumeration<String> attributeNames = vS.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                logger.info("Attribute in session {}: {}", attributeName, vS.getAttribute(attributeName));
            }

            return "cephx.jsp" + instant + sharedPatientParameter;
        }
        if (vLoginType == funcclass.ADMIN) {
            SessionUtility.setAdmin(vS); // admin security
            logger.info("Redirecting to admin page");
            return "_admin/Admin_cephX.jsp";
        }
        if (vLoginType == funcclass.GUEST || vLoginType == funcclass.SHARE_ALL_R || vLoginType == funcclass.SHARE_ALL_RW || vLoginType == funcclass.SHARE_ONE_R || vLoginType == funcclass.SHARE_ONE_RW) {
            if (vLoginType == funcclass.GUEST) {
                cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_MULTI);
            } else if (vLoginType == funcclass.SHARE_ALL_R) {
                cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_MULTI);
            } else if (vLoginType == funcclass.SHARE_ALL_RW) {
                cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL_MULTI);
            } else if (vLoginType == funcclass.SHARE_ONE_R) {
                cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_SINGLE);
            } else if (vLoginType == funcclass.SHARE_ONE_RW) {
                cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL_SINGLE);
            }

            long sh = UserAccounts.getDBConnection().getSharedNum(vUsr, vPass);
            //vS.setAttribute(UserAccountsConsts.PRM_LOCATION,"_doctor/guest.jsp?share="+sh);
            //return UserAccountsConsts.PG_APP_REDIRECT;
            return "_doctor/guest.jsp?share=" + sh;
        }
        if (vLoginType == funcclass.PATIENT) {
            //vS.setAttribute(UserAccountsConsts.PRM_LOCATION,"_patient/site.jsp");
            vS.setAttribute("patId", vPass);
            //return UserAccountsConsts.PG_PARENT_REDIRECT;
            return "_patient/site.jsp?patId=" + vPass;
        }
        logger.info("Return error page from login action");
        return UserAccountsConsts.PG_ERROR_PAGE;
    }

    private BasicDoc createNewDoctorInfo(HttpServletRequest vReq) {
        BasicDoc bd = new BasicDoc();
        bd.name = vReq.getParameter("fname");
        bd.lastname = vReq.getParameter("lname");
        bd.email = vReq.getParameter("email");
        bd.user = bd.email;
        bd.password = vReq.getParameter("password");
        logger.info("createNewDoctorInfo   User: " + bd.user + " Password: " + bd.password);
        bd.address = vReq.getParameter("address");
        bd.address2 = vReq.getParameter("address2");
        bd.city = vReq.getParameter("city");
        bd.state = vReq.getParameter("state");
        bd.country = vReq.getParameter("country");

//        if (bd.country.equals("Israel") || bd.country.equals("IL")) {
//            bd.setScaleEstimation(true);
//        }
        bd.setScaleEstimation(true);

        bd.phoneNumber = vReq.getParameter("phone");
        bd.fax = vReq.getParameter("fax");
        bd.zip = vReq.getParameter("zip");
        bd.partnerId = vReq.getParameter("partner_id") == null || !StringUtils.isNumeric(vReq.getParameter("partner_id").trim()) ? -1 : Long.parseLong(vReq.getParameter("partner_id").trim());
        bd.userType = Integer.valueOf(vReq.getParameter("utype"));
        bd.authorized = true;
        bd.isOrto2 = Boolean.valueOf(vReq.getParameter("isOrto2"));
        bd.extDoctorId = vReq.getParameter("ext_doctor_id") != null ? vReq.getParameter("ext_doctor_id") : "";
        if (bd.extDoctorId.isEmpty()) {
            bd.extDoctorId = vReq.getParameter("ext_doc_ID") != null ? vReq.getParameter("ext_doc_ID") : "";
        }

        if (StringUtils.isNotBlank(vReq.getParameter("cost_per_case"))) {
            bd.costPerCase = Double.parseDouble(vReq.getParameter("cost_per_case"));
        }

        if (StringUtils.isNotBlank(vReq.getParameter("monthly_fee"))) {
            bd.monthlyCost = Double.parseDouble(vReq.getParameter("monthly_fee"));
        }

        if (StringUtils.isNotBlank(vReq.getParameter("credits"))) {
            bd.freeAnalyses = Integer.parseInt(vReq.getParameter("credits"));
        } else {
            bd.freeAnalyses = 0;
        }

        // default behavior
        bd.isAlgoCeph = true;
        bd.allowedToTrace = true;
        bd.sync = "bbb".equals(vReq.getParameter("billing_method")) ? 1 : 0 ;

        return bd;
    }

    private void clearSession(HttpSession vS) {
        java.util.Enumeration e = vS.getAttributeNames();
        while (e.hasMoreElements()) {
            System.out.println("ELEMS IN SESION $$$$$ " + (String) e.nextElement());
        }

    }

    public void getDocsInExcel(HttpServletRequest vReq, HttpServletResponse vRes) {
        Vector alls = new Vector();
        SessionUtility.noCache(vRes);
        PrintWriter out;
        try {
            out = vRes.getWriter();
        } catch (Exception e) {
            return;
        }

        try {
            alls = UserAccounts.getDBConnection().getDocInfoList();
        } catch (Exception e) {
            out.println("<HTML><BODY><P align='center'><H2>Can't access doctors list</H2></P></BODY></HTML>");
            return;
        }

        vRes.setContentType("application/vnd.ms-excel");
        vRes.setHeader("Content-Disposition", "attachment;filename=test.xls");
        out.println(BasicDoc.getExcelTableOrder());
        for (int i = 0; i < alls.size(); ++i) {
            out.println(((BasicDoc) alls.elementAt(i)).toExcelRecord());
        }
    }

    public void getPDF(HttpServletRequest vReq, HttpServletResponse vRes) {
        vRes.setContentType("application/pdf");
        vRes.setHeader("Content-Disposition", "filename=test.pdf;");
        long patnum = Long.parseLong(vReq.getParameter("patnum"));

        try {
            new pdf(patnum, null, new CountingOutputStream(vRes.getOutputStream()), "", "", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getEcephPDF(HttpServletRequest vReq, HttpServletResponse vRes) {

        long compnum = -1;
        long logo_doc_num = 0;
        long patnum = Long.parseLong(vReq.getParameter("patnum"));
        long docnum = Long.parseLong(vReq.getParameter("docnum"));//addedd

        String company = vReq.getParameter("company");

        if (company != null && !company.trim().equals("") && !company.trim().equalsIgnoreCase("null")) {
            try {
                compnum = Long.parseLong(company);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (compnum > 0) {
            logo_doc_num = compnum;
        } else {
            logo_doc_num = docnum;
        }

        String PatName = vReq.getParameter("patname");
        String docName = vReq.getParameter("DocName");

        vRes.setContentType("application/pdf");
        vRes.setHeader("Content-Disposition", "attachment;filename=" + getDownFileName(PatName, patnum) + ".pdf;");
        StringTokenizer st = new StringTokenizer(vReq.getParameter("data"), UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

        try {

            new pdf(patnum, ret, new CountingOutputStream(vRes.getOutputStream()), docName, cn.getDocPreferences(logo_doc_num).getLogoPath(), false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getImageTst(HttpServletRequest vReq, HttpServletResponse vRes) throws Exception {
        int w = Integer.parseInt(vReq.getParameter("w"));
        int h = Integer.parseInt(vReq.getParameter("h"));
        BufferedImage img = draw("This is graphics converted to img");//new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        vRes.setContentType("image/jpeg");
        try {
            OutputStream out = vRes.getOutputStream();
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
            JPEGEncodeParam param =
                encoder.getDefaultJPEGEncodeParam(img);
            param.setQuality(1.0f, true);
            encoder.encode(img, param);
            out.close();
        } catch (Exception e) {
            System.out.println("Can't create Image from graphics " + e);
        } finally {
            g2.dispose();
        }

    }

    protected BufferedImage draw(String st) {
        int WIDTH = 200;
        int HEIGHT = 200;
        int FONTSIZE = 11;
        String FONTNAME = "Arial";
        BufferedImage img =
            new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setBackground(Color.orange);
        g2.clearRect(0, 0, WIDTH, HEIGHT);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.black);
        Font font = new Font(FONTNAME, Font.PLAIN, FONTSIZE);
        g2.setFont(font);
        TextLayout tl =
            new TextLayout(st, font,
                g2.getFontRenderContext());
        Rectangle2D r = tl.getBounds();
        // center the text
        tl.draw(g2, (float) ((WIDTH - r.getWidth()) / 2),
            (float) (((HEIGHT - r.getHeight()) / 2) +
                r.getHeight()));
        g2.dispose();
        return img;
    }


    public void getSmartPDF_print(HttpServletRequest vReq, HttpServletResponse vRes) {

        long patnum = 0;
        long docnum = 0;
        String PatName = "";
        String docName = "";
        String showLogo = "true";
        String pdfData = "";


        try {
            HttpSession ses = vReq.getSession();
            patnum = Long.parseLong((String) ses.getAttribute("PDFpatnum"));
            docnum = Long.parseLong((String) ses.getAttribute("PDFdocnum"));
            PatName = (String) ses.getAttribute("PDFpatname");
            docName = (String) ses.getAttribute("PDFDocName");

            showLogo = "true";
            String tmpLogo = (String) ses.getAttribute("PDFshowLogo");
            if (tmpLogo != null) {
                showLogo = tmpLogo;
            }

            pdfData = (String) ses.getAttribute("PDFdata");

            //remove all attributes after print

            ses.removeAttribute("PDFpatnum");
            ses.removeAttribute("PDFdocnum");
            ses.removeAttribute("PDFpatname");
            ses.removeAttribute("PDFDocName");
            ses.removeAttribute("PDFshowLogo");
            ses.removeAttribute("PDFdata");
        } catch (Exception e) //if no session data was stored and worked on params only
        {
            patnum = Long.parseLong(vReq.getParameter("patnum"));
            docnum = Long.parseLong(vReq.getParameter("docnum"));//addedd
            PatName = vReq.getParameter("patname");
            docName = vReq.getParameter("DocName");
            showLogo = "true";

            if (vReq.getParameter("showLogo") != null) {
                showLogo = vReq.getParameter("showLogo");
            }

            pdfData = vReq.getParameter("data");
        }

        //SessionUtility.noCache(vRes);

        vRes.setContentType("application/pdf");
        vRes.setHeader("Content-Disposition", "inline; filename=" + getDownFileName(PatName, patnum) + ".pdf;");
        //vRes.setHeader("Content-Type","application/pdf");
        //StringTokenizer st = new StringTokenizer(vReq.getParameter("data"),UserAccountsConsts.DEF_DELIM);
        StringTokenizer st = new StringTokenizer(pdfData, UserAccountsConsts.DEF_DELIM);
        String[] ret = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            ret[i++] = st.nextToken();
        }
        DBconnection cn = UserAccounts.getDBConnection();

		/*
        long docnum = 0;

        try
        {
            docnum = cn.getDocInfoFromPatientNum(patnum);
            //cn.setDoctorAsOwner(patnum);

        }
        catch(Exception e)
            {e.printStackTrace(); }

		 */

        try {
            System.out.println("PDF requested for: \n docname[" + docName + "] \nlogo :" + cn.getDocPreferences(cn.getDocInfoFromPatientNum(patnum)).getLogoPath() + "]\nstream: " + vRes.getOutputStream() + "\nret: " + ret.toString() + "\npatId: " + patnum);
            new pdf(patnum, ret, new CountingOutputStream(vRes.getOutputStream()), docName, cn.getDocPreferences(docnum).getLogoPath(), new Boolean(showLogo).booleanValue());

            //new pdf(patnum,ret,vRes.getOutputStream(),docName,cn.getDocPreferences(cn.getDocInfoFromPatientNum(patnum)).getLogoPath(),showLogo.equals("true"));

        } catch (Exception e) {
            System.out.println("PDF PRINT  " + e);
            e.printStackTrace();
        }
    }


    public void getExcel(HttpServletRequest vReq, HttpServletResponse vRes) {
        String PatName = "PatientAnalysis";

        if (vReq.getParameter("patname") != null) {
            PatName = vReq.getParameter("patname");
        }
        long PatNum = Long.parseLong(vReq.getParameter("patnum"));

        vRes.setContentType("application/vnd.ms-excel");
        vRes.setHeader("Content-Disposition", "attachment;filename=" + getDownFileName(PatName, PatNum) + ".xls;");
        //vRes.setHeader("Content-Disposition", "attachment;filename="+PatName+".xls;");
        PrintWriter out = null;
        try {
            out = vRes.getWriter();
        } catch (Exception e) {
        }
        ExcelBuilder eb = new ExcelBuilder(out, PatNum);
        out.close();
    }

    public void getPatTxAttach(HttpServletRequest vReq, HttpServletResponse vRes) {
        long patnum = Long.parseLong(vReq.getParameter("patId"));
        String patName = vReq.getParameter("patName");

        TxAttach ta = new TxAttach(vReq.getParameter("patId"));
        File f = ta.getUserFile();
        if (ta == null) {
            logger.info("file streamer download user TxAttachment was failed for patient " + patName + " [" + patnum + "]");
            return;
        }

        vRes.setContentType("application/octet-stream");
        vRes.setHeader("Content-Disposition", "attachment;filename=" + getDownFileName(patName, patnum) + ta.getFileExtention(f.getName()));

        //vRes.setHeader("Content-Disposition", "attachment;filename="+patName+ta.getFileExtention(f.getName()));
        try {
            FileInputStream fis = new FileInputStream(f);
            byte[] ba = new byte[fis.available()];
            //byte[] ba = new byte[0];
            fis.read(ba);
            OutputStream os = vRes.getOutputStream();
            os.flush();
            os.write(ba);
            fis.close();
        } catch (Exception e) {
            logger.info("File streamer download user TxAttachment was failed for patient " + patName + " [" + patnum + "] by the following reasons: \n");
            system.printStackTrace(e);
        }
    }

    private boolean isHacker(String vUser, String vPass) {
        if (isBadCharacters(vUser) || isBadCharacters(vPass)) {
            return true;
        }
        return false;
    }

    private boolean isBadCharacters(String vStr) {
        if (vStr.indexOf("\"") != -1 || vStr.indexOf("'") != -1) {
            return true;
        }
        return false;
    }

    private String getDownFileName(String vName, long vNum) {

        String badChars = "~!@#$%^&*()-+=}{[]\"':;?><,`\\";
        for (int i = 0; i < vName.length(); ++i) {

            if (badChars.indexOf("" + vName.charAt(i)) != -1) {

                vName = vName.replace(vName.charAt(i), ' ');
            }
        }

        if (vName.trim().equals("")) {
            vName = "cephX patient " + vNum;
        }

        return vName.trim();
    }

    public void randomizeNewPassword(HttpServletRequest vReq, HttpServletResponse vRes) {
        String userName = (String) vReq.getParameter("username");

        DBconnection db = DBconnection.GetDBconnection();
        try {

            long doc_id = DoctorService.getDoctorNumberFromUser(userName);
            String email = DoctorService.getDoctorEmail(doc_id);
            String password = db.resetPasswordAndRandomizeNewOne(userName);

            MailService.sendMail("Your password was reset as requested. the new password is: " + password, email, "Cephx System - Password Reset", false);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void passwordRecovery(HttpServletRequest vReq, HttpServletResponse vRes) {
        String userOrEmail = vReq.getParameter("email");
        String email;
        DBconnection db = DBconnection.GetDBconnection();
        PrintWriter writer = null;
        try {
            StringBuilder sb = new StringBuilder();
            vRes.setContentType("text/plain");
            writer = vRes.getWriter();
            email = db.getDocMailByUserOrMail(userOrEmail);
            if (email != null) {
                long docNum = DoctorService.getDoctorNumberFromUser(email);
                BasicDoc bd = db.getDocInfo(docNum);
                String hash = UUID.randomUUID().toString() + "-" + (new Date().getTime()) + docNum;
                db.savePasswordRecovery(docNum, hash);
                sb.append("<div style='text-align:center'><img src='https://cloud.cephx.com/cephx/dLogo/Logo_CEPHX.png' width=143 height=80  border=0> </div>");
                sb.append("<p><b>Hi " + bd.getDocFullName() + ",</b></p>");
                sb.append("<br></br>");
                sb.append("<br></br>");
                sb.append("<br></br>");
                sb.append("CephX recently received a request for a forgotten password.To change your CephX password, please click on this <a href='" + funcclass.baseUrl + "passwordRecovery.jsp?hash=" + hash + "'>link</a>.If you did not request this change, you do not need to do anything.");
                sb.append("<br></br>");
                sb.append("<br></br>");
                sb.append("<br></br>");
                sb.append("<p>Thanks,");
                sb.append("<br>CephX Team</p>");
                MailService.sendMail(email, "support@cephx.com", "Password recovery", sb.toString(), true /* "To recovery password, please follow this link " + funcclass.baseUrl + "passwordRecovery.jsp?hash=" + hash*/);
                vRes.setStatus(HttpServletResponse.SC_OK);
                writer.print("true");
            } else {
                vRes.setStatus(HttpServletResponse.SC_OK);
                writer.print("false");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            vRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.print(ex.getMessage());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void passwordRecoveryConfirm(HttpServletRequest vReq, HttpServletResponse vRes) {
        String hash = vReq.getParameter("hash");
        String password = vReq.getParameter("pass");
        vRes.setContentType("text/plain");

        DBconnection db = DBconnection.GetDBconnection();
        try {
            try {
                Long docId = db.getDocIdFromasswordRecovery(hash);
                if (docId == null) {
                    throw new Exception("Hash doesn't exists or expired");
                }
                String passwordHash = PasswordHash.createHash(password);
                db.updateDocPassword(docId, passwordHash);
                vRes.getWriter().print("Password successfully changed, you can now login to your account");
            } catch (SQLException ex) {
                vRes.getWriter().print("Some error. Try again later");
                ex.printStackTrace();
            } catch (Exception e) {
                vRes.getWriter().print(e.getMessage());
            } finally {
                vRes.getWriter().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean userExists(final String user) {
        return db.checkIfUserOrEmailExist(user);
    }

    public Long createDoctorFromCode(final String doctorFirstName, final String doctorLastName, final String doctorEmail, final String billingMethod, final String billingPlanId, final String billingPlanType, final String phone, final String country, final String password, final String zohoId) {
        Long doctorId = null;
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URI url = addNewDoctorUrl();
            final HttpPost post = new HttpPost();
            post.setURI(url);
            final List<NameValuePair> requestParameters = new ArrayList<>();
            requestParameters.add(new BasicNameValuePair(ACTION_PARAMETER_NAME, "insertNewDoc"));
            requestParameters.add(new BasicNameValuePair(FIRST_NAME_PARAMETER_NAME, doctorFirstName));
            requestParameters.add(new BasicNameValuePair(LAST_NAME_PARAMETER_NAME, doctorLastName));
            requestParameters.add(new BasicNameValuePair(EMAIL_PARAMETER_NAME, doctorEmail));
            requestParameters.add(new BasicNameValuePair(PASSWORD_PARAMETER_NAME, password));
            requestParameters.add(new BasicNameValuePair(USERTYPE_PARAMETER_NAME, "1"));
            requestParameters.add(new BasicNameValuePair(IS_ORTO2_PARAMETER_NAME, "false"));
            requestParameters.add(new BasicNameValuePair(PAYMENT_METHOD_PARAMETER_NAME, billingMethod));
            if ("bbb".equals(billingMethod) && !"free_plan".equals(billingPlanId)) {
//                requestParameters.add(new BasicNameValuePair(BILLING_PLAN_ID_PARAMETER_NAME, billingPlanId));
//                requestParameters.add(new BasicNameValuePair(BILLING_PLAN_TYPE_PARAMETER_NAME, billingPlanType));
            }
       //     requestParameters.add(new BasicNameValuePair(API_KEY_PARAMETER_NAME, getApiKeyByEnvironment()));
            requestParameters.add(new BasicNameValuePair(PHONE_PARAMETER_NAME, phone));
            requestParameters.add(new BasicNameValuePair(COUNTRY_PARAMETER_NAME, country));
            requestParameters.add(new BasicNameValuePair(ZOHO_ID_PARAMETER_NAME, zohoId));
            post.setEntity(new UrlEncodedFormEntity(requestParameters));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            final HttpResponse addDoctorResponse = client.execute(post);
            final JSONObject replyJSON = new JSONObject(EntityUtils.toString(addDoctorResponse.getEntity()));
            final Boolean userExists = replyJSON.getBoolean("user_already_exists");
            doctorId = replyJSON.getLong("cephxId");

        } catch (IOException e) {
            logger.error(e);
        } catch (URISyntaxException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
        return doctorId;
    }

    private String getApiKeyByEnvironment() {
        String apiKey = "";
        if (funcclass.isProdEnvironment()) {
            apiKey = null;
        }
        if (funcclass.isQaEnvironment() || funcclass.isLocalHost()) {
            apiKey = partnerService.getPartnerByName("Qa server").getApiKey();
        }
        return null;
    }

    private URI addNewDoctorUrl() throws URISyntaxException {
        return new URI(funcclass.baseUrl + "servlet/UserAccounts");
    }

}
