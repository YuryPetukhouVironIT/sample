package com.company.def.service.webapi;

import com.company.def.BasicDoc;
import com.company.def.Cdoctor;
import com.company.def.DBconnection;
import com.company.def.enums.BillingMethod;
import com.company.def.exceptions.WebApiException;
import com.company.def.funcclass;
import com.company.def.service.AuthenticationService;
import com.company.def.service.db.DoctorService;
import com.company.def.service.db.PatientService;
import com.company.def.servlets.admin.AdminService;
import com.company.def.servlets.admin.Partner;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import static com.company.def.service.AccountService.ACTION_PARAMETER_NAME;
import static com.company.def.service.AccountService.API_KEY_PARAMETER_NAME;
import static com.company.def.service.AccountService.DOCTOR_ID_PARAMETER_NAME;
import static com.company.def.service.AccountService.EMAIL_PARAMETER_NAME;
import static com.company.def.service.AccountService.FIRST_NAME_PARAMETER_NAME;
import static com.company.def.service.AccountService.IS_ORTO2_PARAMETER_NAME;
import static com.company.def.service.AccountService.LAST_NAME_PARAMETER_NAME;
import static com.company.def.service.AccountService.MASTER_ID_PARAMETER_NAME;
import static com.company.def.service.AccountService.PASSWORD_PARAMETER_NAME;
import static com.company.def.service.AccountService.PAYMENT_METHOD_PARAMETER_NAME;
import static com.company.def.service.AccountService.USERTYPE_PARAMETER_NAME;

@Service
public class PartnerPatientServiceImpl implements PartnerPatientService {

    private static final Logger logger = LogManager.getLogger(PartnerPatientServiceImpl.class);
    private static final DBconnection dbConnection = DBconnection.GetDBconnection();

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public String downloadPage(final HttpServletRequest request) {
        String page = "";
        try {
            final Long doctorNumber = dbConnection.getDoctorByExternalId(request.getParameter("ext_doctor_id"));
            final Long patientNumber = Long.parseLong(dbConnection.getPatientNumberByExternalPatientId(request.getParameter("ext_patient_id"), String.valueOf(doctorNumber)));

            request.getSession().setAttribute("patient_number", patientNumber);
            request.getSession().setAttribute("doctor_number", doctorNumber);
            final BasicDoc docInfo = dbConnection.getDocInfo(doctorNumber);
            boolean docHasCredits = true;
            if (!docInfo.hasBillingMethod() && (docInfo.prepaidAnalyses + docInfo.freeAnalyses) == 0) {
                docHasCredits = false;
            }
            if (!PatientService.isAlgoCephHotPotato(patientNumber) && !dbConnection.isAlgoCephFailedForPatient(patientNumber) && docInfo.isAlgoCeph && ((docInfo.allowedToTrace && docInfo.isLegit) || docHasCredits) && PatientService.getPatientOwner(patientNumber) == 1) {
                page = "patientdownloadpage";
            } else {
                page = "algoFail";
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return page;
    }

    private void addDoctorToCompany(final Long companyId, final Long cephxId) {
        AdminService.addDoctorToCompany(companyId, cephxId.toString());
    }

    private URI addNewDoctorUrl() throws URISyntaxException {
        return new URI(funcclass.baseUrl + "servlet/UserAccounts");
    }

    @Override
    public void passInputParameters(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Long externalDoctorId = Long.parseLong(request.getParameter("ext_doctor_id"));
        final String partnerIdString = request.getParameter("partner_id");
        final Long partnerId = partnerIdString == null ? null : Long.parseLong(partnerIdString);
        final Partner partner = dbConnection.getPartner(partnerId);
        if (partner == null) {
            throw new WebApiException("Wrong partner Id " + partnerId);
        }
        final long masterAccountId = partner.getMasterId();
        final String token = request.getParameter("auth_token");
        if (masterAccountId == -1 || !(dbConnection.getDocInfo(masterAccountId).user.equals(authenticationService.checkApiTokenAuthentication(request, response)))) {
            throw new WebApiException("Wrong master account");
        }
        if (!dbConnection.isExtDoctorIdExist(String.valueOf(externalDoctorId))) {

            try {
                final String billingMethod = BillingMethod.MASTER_ID;
                createExternalDoctor(partner, masterAccountId, externalDoctorId, billingMethod, request);
            } catch (Exception e) {
                logger.error(e);
            }

        }
        final Long doctorId = dbConnection.getDoctorByExternalId(String.valueOf(externalDoctorId));
        if (doctorId==null) {
            throw new WebApiException("Failed to create doctor with external id "+externalDoctorId);
        }
        request.getSession().setAttribute("doctor_id", doctorId);
        request.getSession().setAttribute("doctor_number", doctorId);
        request.getSession().setAttribute("auth_token", token);

        if (!checkDoctorsInCompany(masterAccountId, doctorId)) {
            throw new WebApiException("External doctor " + externalDoctorId + " is not in company of master account");
        }
        try {
            final Cdoctor DocObj = new Cdoctor(doctorId);
            DocObj.docInfo = dbConnection.getDocInfo(doctorId);
            request.getSession().setAttribute("DocObj", DocObj);
            request.getSession().setAttribute("isForceCompany", dbConnection.getIsForceCompany(DocObj.getDocNumber()));
            request.getSession().setAttribute("currentLanguageCode", DoctorService.getDoctorLanguage(DocObj.docInfo.user));
            String logoDownloadPath = partner.getRelativeLocalLogoPath();
            request.getSession().setAttribute("partner_logo_src", logoDownloadPath);
        } catch (Exception e) {
            logger.error(e);
            request.getSession().setAttribute("currentLanguageCode", "en");
        }
    }

    private boolean checkDoctorsInCompany(final Long masterAccountId, final Long externalDoctorId) {
        boolean doctorInCompany = false;
        final Vector<BasicDoc> companyDoctors = dbConnection.getCompanyDocInfos(masterAccountId);
        final Iterator<BasicDoc> iterator = companyDoctors.iterator();
        while ((!doctorInCompany) && iterator.hasNext()) {
            doctorInCompany = iterator.next().docnum == externalDoctorId;
        }
        return doctorInCompany;
    }

    private void createExternalDoctor(final Partner partner, final Long companyId, final Long doctorId, final String billingMethod, final HttpServletRequest request) {
        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final URI url = addNewDoctorUrl();

            final String partnerName = partner.getName();
            final HttpPost post = new HttpPost();
            post.setURI(url);
            final List<NameValuePair> requestParameters = new ArrayList<>();
            final String doctorFirstName = request.getParameter("doc_first_name") != null ? request.getParameter("doc_first_name") : partnerName;
            final String doctorLastName = request.getParameter("doc_last_name") != null ? request.getParameter("doc_last_name") : partnerName;
            String doctorEmail = request.getParameter("doc_email");
            if (doctorEmail == null) {
                int attempts = 100;
                String randomEmail = getRandomEmail(partnerName);
                boolean uuidIsUnique = false;
                while (attempts > 0 && uuidIsUnique) {
                    try {
                        uuidIsUnique = !dbConnection.checkIfdocExists(randomEmail, randomEmail, 0);
                        if (!uuidIsUnique) {
                            randomEmail = getRandomEmail(partnerName);
                            attempts--;
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
                doctorEmail = randomEmail;
            }
            requestParameters.add(new BasicNameValuePair(ACTION_PARAMETER_NAME, "insertNewDoc"));
            requestParameters.add(new BasicNameValuePair(FIRST_NAME_PARAMETER_NAME, doctorFirstName));
            requestParameters.add(new BasicNameValuePair(LAST_NAME_PARAMETER_NAME, doctorLastName));
            requestParameters.add(new BasicNameValuePair(EMAIL_PARAMETER_NAME, doctorEmail));
            requestParameters.add(new BasicNameValuePair(PASSWORD_PARAMETER_NAME, RandomStringUtils.random(16, 65, 90, true, true)));
            requestParameters.add(new BasicNameValuePair(USERTYPE_PARAMETER_NAME, "1"));
            requestParameters.add(new BasicNameValuePair(IS_ORTO2_PARAMETER_NAME, "false"));
            requestParameters.add(new BasicNameValuePair(API_KEY_PARAMETER_NAME, partner.getApiKey()));
            requestParameters.add(new BasicNameValuePair(DOCTOR_ID_PARAMETER_NAME, String.valueOf(doctorId)));
            requestParameters.add(new BasicNameValuePair(PAYMENT_METHOD_PARAMETER_NAME, billingMethod));
            requestParameters.add(new BasicNameValuePair(MASTER_ID_PARAMETER_NAME, String.valueOf(partner.getMasterId())));
            post.setEntity(new UrlEncodedFormEntity(requestParameters));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            final HttpResponse addDoctorResponse = client.execute(post);

            final JSONObject replyJSON = new JSONObject(EntityUtils.toString(addDoctorResponse.getEntity()));
            final Boolean userExists = replyJSON.getBoolean("user_already_exists");
            final Long cephxId = replyJSON.getLong("cephxId");
            if (!userExists) {
                addDoctorToCompany(companyId, cephxId);
            }
        } catch (IOException e) {
            logger.error(e);
        } catch (URISyntaxException e) {
            logger.error(e);
        } catch (JSONException e) {
            logger.error(e);
        }
    }

    private String getRandomEmail(final String partnerName) {
        String randomEmail = UUID.randomUUID().toString() + "@" + partnerName + ".com";        ;
        if (randomEmail.length()>50) {
            randomEmail = randomEmail.substring(randomEmail.length()-50);
        }
        return randomEmail;
    }

    @Override
    public long checkPatient(final HttpServletRequest request) {
        final String externalPatientId = request.getParameter("ext_patient_id");
        final String externalDoctorId = request.getParameter("ext_doctor_id");
        final Long doctorId = dbConnection.getDoctorByExternalId(externalDoctorId);
        final String patientId = dbConnection.getPatientNumberByExternalPatientId(externalPatientId, String.valueOf(doctorId));
        if (doctorId==null || StringUtils.isBlank(patientId)) {
            throw new WebApiException("Wrong external patient id " + externalPatientId);
        }
        return Long.parseLong(patientId);
    }

    @Override
    public void checkIfPatientExists(HttpServletRequest request) {
        final String externalPatientId = request.getParameter("ext_patient_id");
        final String externalDoctorId = request.getParameter("ext_doctor_id");
        final Long doctorId = dbConnection.getDoctorByExternalId(externalDoctorId);
        if (doctorId != null) {
            final String patientId = dbConnection.getPatientNumberByExternalPatientId(externalPatientId, String.valueOf(doctorId));
            if (!StringUtils.isBlank(patientId)) {
                throw new WebApiException("Patient with external id " + externalPatientId + " already exists for this doctor");
            }
        }
    }
}
