package com.cephx.def.service;

import com.Ostermiller.util.Base64;
import com.cephx.def.BasicDoc;
import com.cephx.def.Cdoctor;
import com.cephx.def.DBconnection;
import com.cephx.def.funcclass;
import com.cephx.def.model.StlViewerToken;
import com.cephx.def.repository.StlViewerTokenRepository;
import com.cephx.def.servlets.accounts.PasswordHash;
import com.cephx.def.system;
import com.cephx.def.util.string.StringUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.Calendar;
import java.util.Date;

@Service
public class AuthenticationService {

    private static final DBconnection dbConnection = DBconnection.GetDBconnection();
    private static final Logger logger = LogManager.getLogger(AuthenticationService.class);

    @Autowired
    private StlViewerTokenRepository stlViewerTokenRepository;

    public boolean checkTokenAuthentication(final String authToken, final long accountId) {
        boolean autenticated = false;
        try {
            final BasicDoc docInfo = dbConnection.getDocInfo(accountId);
            final String tokenInBase = Base64.encode(docInfo.user + docInfo.password);
            autenticated = authToken.toUpperCase().equals(StringUtility.MD5(tokenInBase).toUpperCase());
        } catch (Exception e) {
            logger.error(e);
        }
        return autenticated;
    }

    public String checkApiTokenAuthentication(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        if (req.getSession().getAttribute("authToken") != null && req.getSession().getAttribute("DocObj") != null) {
            final Long doctorId = ((Cdoctor) req.getSession().getAttribute("DocObj")).DocNum();
            if (checkTokenAuthentication(req.getSession().getAttribute("authToken").toString(), doctorId)) {
                return dbConnection.getDocInfo(doctorId).user;
            } else {
                return null;
            }

        }
        boolean isJsonContent = "application/json".equals(req.getContentType());
        JSONObject jsonRequestBody = null;
        if (isJsonContent) {
            try (BufferedReader br = req.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                jsonRequestBody = new JSONObject(sb.toString());
            } catch (JSONException e) {
                logger.error("Error", e);
                system.printStackTrace(e);
            }
        }
        String authToken = null;
        try {
            authToken = req.getParameter("authToken");
            if (authToken == null) {
                authToken = req.getParameter("auth_token");
            }
            if (authToken == null || authToken.isEmpty()) {
                authToken = isJsonContent ? jsonRequestBody.getString("authToken") : req.getParameter("authToken");
            }
        } catch (JSONException e) {
            logger.error("Error", e);
            system.printStackTrace(e);
        }

        String docUserName = validateAuthentication(authToken);

        if (docUserName == null) {
            logger.warn("Problem with authentication token");
            int responseCode = 401;
            resp.sendError(responseCode, "Problem occured when validating the authtoken, make sure the token is valid");
            return null;
        }
        return docUserName;
    }

    public String validateAuthentication(String authToken) {

        String docUserName = null;
        String docPass = null;

        String docAuthDetails = Base64.decode(authToken);
        try {
            docUserName = docAuthDetails.split("\\+")[0];
            docPass = docAuthDetails.split("\\+")[1];
            String md5Pass = PasswordHash.createMD5Hash(docPass);

            if (funcclass.isLocalHost()) {
                //      md5Pass = docPass;
            }

            String goodHash = PasswordHash.getPassHashFromDb(docUserName);
            if (!PasswordHash.validatePassword(md5Pass, goodHash)) {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error", e);
            system.printStackTrace(e);
            return null;
        }

        return docUserName;
    }

    public boolean checkViewerAuthenticationToken(final String authToken) {
        final StlViewerToken viewerToken = stlViewerTokenRepository.findFirstByToken(authToken);
        if (viewerToken == null) {
            logger.error("Failed to make STL viewer authentication with token:"+authToken);
            return false;
        }
        if (viewerToken.getExpirationDateTime().before(new Date())) {
            logger.error("Expired STL viewer authentication token:"+authToken);
            stlViewerTokenRepository.delete(viewerToken);
            return false;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        viewerToken.setExpirationDateTime(cal.getTime());
        stlViewerTokenRepository.save(viewerToken);
        return true;
    }
}
