package com.cephx.def.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.cephx.def.*;
import com.cephx.def.enums.BillingMethod;
import com.cephx.def.model.DoctorLogin;
import com.cephx.def.service.db.*;
import com.cephx.def.servlets.accounts.PasswordHash;
import com.cephx.def.servlets.accounts.UserAccounts;
import com.cephx.def.servlets.accounts.UserAccountsConsts;
import com.cephx.def.servlets.patient.ImageConverter;
import com.cephx.def.util.file.FileUtility;
import com.cephx.def.util.string.StringUtility;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.cephx.def.servlets.admin.Partner;
import com.cephx.def.servlets.util.BrowserInfo;
import com.cephx.def.servlets.util.CephxSession;
import com.cephx.def.servlets.util.SessionUtility;
import com.cephx.def.struct.struct.TxAttach;
import com.cephx.def.tools.ExcelBuilder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class UserAccountsService {
	private static final long CT_DENT_MASTER_ID = 320L;
	private final static Logger logger = LogManager.getLogger(UserAccountsService.class);

	public static void insertNew(HttpServletRequest vReq, HttpServletResponse vRes) {
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
			if (vReq.getParameter("instant") != null)
				if (vReq.getParameter("instant").equals("true")) {
					session.setAttribute("instant", "true");
					instant = "?instant=true";
				} else {
					instant = "";
				}


			BasicDoc dc = createNewDoctorInfo(vReq);

			DBconnection cn = UserAccounts.getDBConnection();

			String apiKey = vReq.getParameter("api_key");
			Partner partner;
			if (apiKey == null || (partner = PartnerService.getPartnerByAPiKey(apiKey)) == null) {
				logger.warn("Invalid api key {}", apiKey);
				vRes.setStatus(HttpServletResponse.SC_FORBIDDEN);
				vRes.setHeader("Message", "Invalid api key");
				return;
			}

			if (dc.extDoctorId != null && !dc.extDoctorId.isEmpty() && cn.isExtDoctorIdExist(dc.extDoctorId)) {
				jsonError.put("redirect_url", UserAccountsConsts.PG_ERROR_PAGE);
				jsonError.put("user_already_exists", "true");
				jsonError.put("cephxId", -1);
				out.print(jsonError.toString());
				out.close();
				return;
			}

			if (partner.getName().equals("CT-Dent")) {
				dc.country = "IL";
			}

			if (partner.isAutoActiveDoctor()) {
				dc.isAlgoCeph = true;
				dc.isLegit = true;
				dc.allowedToTrace = true;
				dc.billingMethod = BillingMethod.CHARGE_LINK;
				dc.partnerId = partner.getId();
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
			if(vReq.getParameter("billing_method") != null){
				dc.billingMethod = vReq.getParameter("billing_method");
			}
			try {

				// ido: hash + salt pass before insert to DB. save the hash in doctors table
				dc.password = PasswordHash.createHash(dc.password);
				if (vReq.getParameter("doctor_id") != null) {
					dc.docnum = cn.AddDocToDbWithId(dc);
				} else {
					dc.docnum = cn.AddDocToDb(dc);
				}
				if (vReq.getParameter("master_id")!=null) {
					cn.insertNewMasterId(dc.docnum, Long.parseLong(vReq.getParameter("master_id")));
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
			//Add to zoho crm
			else if (!isQaOrLiveServer(vReq, apiKey)) {
				HashMap<String, Object> parameters = new HashMap<>();

				//Partner partner = cn.getPartnerByAPiKey(apiKey);

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
				parameters.put("wfT	rigger", true);

				ZohoCrmClient.addLeadRecord(parameters);
			}

			sendNotificationToSlack(dc, vReq.getRemoteAddr());

			try {// inser default doctor preferences...
				cn.insertDefaultPreferencesForNewDoctor(dc.docnum);
				if (funcclass.isProdEnvironment() || funcclass.isQaEnvironment()) {
					copyPartnerFiltersToDoctor(partner, dc.docnum);
				}
				DoctorService.insertDemoPatientForDoctor(dc.docnum, cn.getPatientImages(funcclass.DEMO_PAT_ID));
				system.log("in UserAccountsService.registerNewDoctor done with setDoctorsFirstPat");
				cn.newDocQuickHelp(dc.docnum);
				system.log("in UserAccountsService.registerNewDoctor done with newDocQuickHelp");
				cn.newDoctorHomePage(dc.docnum);
				system.log("in UserAccountsService.registerNewDoctor done with setDoctorsFirstPat");
			} catch (Exception e) {
				logger.error("Unable to set default preferences for new doctor", e);
			}
			// NewDoc is used mean while to open help popup after new user is created
			if (!dc.isOrto2) {
				session.setAttribute("IamNewDoctor", "NewDoc");
			}
			//	        assigning values to json object
			jsonError.put("redirect_url", doLogin(vReq, dc.user, dc.password, true));
			DoctorService.deleteLogins(dc.docnum);
			jsonError.put("cephxId", dc.docnum);
			jsonError.put("user_already_exists", "false");
			logger.info("Registering new doctor: jsonResponse = {}", jsonError);
			out.print(jsonError.toString());
			out.close();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private static void sendNotificationToSlack(BasicDoc dc, String ipAddress) {
		if(!funcclass.getEnvironment().toLowerCase().equals("dev")) {
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




	/**
	 * Copying filters for new registered doctor from master account of his partner
	 * @param partner Partner of registered doctor
	 * @param doctorNumber Doctor's number
	 */
	private static void copyPartnerFiltersToDoctor(Partner partner, long doctorNumber) {
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

	public static String registerAndLogin(HttpServletRequest vReq, HttpServletResponse vRes) {
		try {
			HttpSession session = vReq.getSession(true);

			String sharedPatientNumber = vReq.getParameter("sharedPatientNumber");
			if (!StringUtils.isEmpty(sharedPatientNumber)) {
				session.setAttribute("sharedPatientNumber", sharedPatientNumber);
			}

			String instant = "";
			if (vReq.getParameter("instant") != null)
				if (vReq.getParameter("instant").equals("true")) {
					session.setAttribute("instant", "true");
					instant = "?instant=true";
				} else
					instant = "";


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
			try {

				// ido: hash + salt pass before insert to DB. save the hash in doctors table
				dc.password = PasswordHash.createHash(dc.password);
				if (vReq.getParameter("ext_doctor_id") != null) {
					dc.docnum = cn.AddDocToDbWithId(dc);
				} else {
					dc.docnum = cn.AddDocToDb(dc);
				}


			} catch (Exception e) {
				logger.error("Can't add doctor to database", e);
				return UserAccountsConsts.PG_ERROR_PAGE;
			}
			//if user already exists
			if (dc.docnum == -1) {
				return UserAccountsConsts.PG_ERROR_PAGE;
			}

			PatientService.updateSharedWithDoctor(dc.email, dc.docnum, dc.getDocFullName());


			if (funcclass.isProdEnvironment() && vReq.getParameter("zohoId") == null) {
				HashMap<String, Object> parameters = new HashMap<>();

				parameters.put("Country", dc.country);
				parameters.put("City", dc.city);
				parameters.put("State", dc.state);
				parameters.put("Email", dc.email);
				parameters.put("First Name", dc.name);
				parameters.put("Last Name", dc.lastname);
				parameters.put("Lead Source", "Share flow");
				parameters.put("Phone", dc.phoneNumber);
				parameters.put("cephX_ID", dc.docnum + "");
				parameters.put("wfTrigger", true);

				ZohoCrmClient.addLeadRecord(parameters);
			}


			sendNotificationToSlack(dc, vReq.getRemoteAddr());

			try {
				cn.insertDefaultPreferencesForNewDoctor(dc.docnum);
                DoctorService.insertDemoPatientForDoctor(dc.docnum, cn.getPatientImages(funcclass.DEMO_PAT_ID));
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

	private static boolean isQaOrLiveServer(HttpServletRequest vReq, String apiKey) {
		//HARD CODE FOR KEY qa and live servers
		return apiKey.equals("cephxlivekey") || apiKey.equals("qa_key_wev6ju");
	}

	public static String doLogin(HttpServletRequest vReq, String vUsr, String vPass, boolean invalidateSession) {

		HttpSession session = vReq.getSession();
		String sharedPatientNumber = (String) session.getAttribute("sharedPatientNumber");

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
			long lastAccessedTime = session.getLastAccessedTime();
			Date date = new Date();
			long currentTime = date.getTime();
			long timeDiff = currentTime - lastAccessedTime;
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
			int allowLogin = 3 - loginAttempt;
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

		String goodHash = PasswordHash.getPassHashFromDb(vUsr);
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
				} else {//
					logger.info("Failed login for user {}", vUsr);
					return UserAccountsConsts.PG_ERROR_PAGE;
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
		system.log("password is valid? " + valid);

		//int logintype = getLoginType(vUsr, vPass); line changed due to password hashing.
		int logintype = 0;
		if (isShare) {
			logintype = getLoginType(vUsr, vPass);
		} else {
			logintype = getLoginType(vUsr, goodHash);
		}


		system.log("in UserAccountsService.doLogin loginType is: " + logintype);
		if (isHacker(vUsr, vPass) || logintype == funcclass.ERROR) {
			system.log("Hack Attepmt - Please note!!!!");
			session.setAttribute(UserAccountsConsts.PRM_ERR_MSG, UserAccountsConsts.TXT_BAD_LOG_INFO);
			return UserAccountsConsts.PG_ERROR_PAGE;
		}

		//*** invalidate current session and get a new session
		try {
			if(invalidateSession)
				session.invalidate();
		} catch (Exception ex){}
		session = vReq.getSession(true);
		session.setAttribute("currentLanguageCode", DoctorService.getDoctorLanguage(vUsr));
		//****
		final String ip = getIp(vReq);
		DBconnection.GetDBconnection().addSessionInfo(ip,vReq.getHeader("User-Agent"),DoctorService.getDoctorNumberFromUser(vUsr));
		return redirectByLoginType(vReq, session, logintype, vUsr, vPass, sharedPatientNumber);
	}

	private static String doLoginLite(HttpServletRequest vReq, String vUsr, String vPass) {

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
		if (company != null && !company.trim().equals(""))
			ret += "?company=" + company;
		final String ip = getIp(vReq);
		DBconnection.GetDBconnection().addSessionInfo(ip,vReq.getHeader("User-Agent"),DoctorService.getDoctorNumberFromUser(vUsr));
		return ret;
	}

	private static String doLoginLiteHeb(HttpServletRequest vReq, String vUsr, String vPass) {

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
		if (company != null && !company.trim().equals(""))
			ret += "?company=" + company;
		final String ip = getIp(vReq);
		DBconnection.GetDBconnection().addSessionInfo(ip,vReq.getHeader("User-Agent"),DoctorService.getDoctorNumberFromUser(vUsr));
		return ret;
	}

	public static String emailLogin(HttpServletRequest vReq) {
		HttpSession session = vReq.getSession(true);
		session.setAttribute(UserAccountsConsts.SES_DIRECT_PATIENT, vReq.getParameter("patnum"));
		session.setAttribute(UserAccountsConsts.SES_START_WITH_PAGE, UserAccountsConsts.PG_LOGIN);
		//session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_APP_INDEX);
		//return 	UserAccountsConsts.PG_APP_REDIRECT;
		return UserAccountsConsts.PG_APP_INDEX;
	}

	public static String doctorExit(HttpServletRequest vReq) {
		HttpSession session = vReq.getSession(true);
		String SPage = vReq.getParameter(UserAccountsConsts.SES_START_WITH_PAGE);
		if (SPage == null)
			SPage = UserAccountsConsts.PG_DEF_START_PAGE;
		session.setAttribute(UserAccountsConsts.SES_START_WITH_PAGE, SPage);
		//session.setAttribute(UserAccountsConsts.PRM_LOCATION,UserAccountsConsts.PG_APP_INDEX);
		//return 	UserAccountsConsts.PG_APP_REDIRECT;
		return UserAccountsConsts.PG_APP_INDEX;
	}

	public static String patLogin(HttpServletRequest vReq) {
		String url = doLogin(vReq, true);
		HttpSession session = vReq.getSession();
		String goToUrl = (String) session.getAttribute(UserAccountsConsts.PRM_LOCATION);
		session.removeAttribute(UserAccountsConsts.PRM_LOCATION);
		return goToUrl;
	}

	public static String sideDoorLogin(HttpServletRequest vReq) {
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

	public static String doLogin(HttpServletRequest vReq, boolean invalidateSession) {
		HttpSession session = vReq.getSession(true);
		String sharedPatientNumber = vReq.getParameter("sharedPatientNumber");
		if (!StringUtils.isEmpty(sharedPatientNumber)) {
			session.setAttribute("sharedPatientNumber", sharedPatientNumber);
		}

		String usr = vReq.getParameter(UserAccountsConsts.PRM_USER);
		String pas = vReq.getParameter(UserAccountsConsts.PRM_PASSWORD);

		if (usr == null || pas == null) {

			return UserAccountsConsts.PG_ERROR_PAGE;
		}
		return doLogin(vReq, usr, pas, invalidateSession);
	}

	public static String doLoginLite(HttpServletRequest vReq) {

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

	public static String doLoginLiteHeb(HttpServletRequest vReq) {

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

	public static void checkCredentials(HttpServletRequest request, HttpServletResponse response) {
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

	private static int getLoginType(String vUser, String vPass) {
		int type = UserAccounts.getDBConnection().getLoginType(vUser, vPass);

		return type;
	}

	private static String redirectByLoginType(HttpServletRequest request, HttpSession vS, int vLoginType, String vUsr, String vPass, String sharedPatientNumber) {
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
			long dnum;
			try {
				dnum = DoctorService.getDoctorNumberFromUser(vUsr);
				logger.info("Doctor {} is logining in system", dnum);
			} catch (Exception e) {
				return UserAccountsConsts.PG_ERROR_PAGE;
			}
			boolean isFirstLogin = DoctorService.isFirstLogin(dnum);
			vS.setAttribute("isFirstLogin", isFirstLogin);
			UserAccounts.getDBConnection().incCounter("login", dnum);
			cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL);
			cs.put(cs.DOC_PREF, UserAccounts.getDBConnection().getDocPreferences(dnum));
			vS.setAttribute("num", String.valueOf(dnum));
			//vS.setAttribute(UserAccountsConsts.PRM_LOCATION,"manager.jsp");
			//vS.setAttribute("accessMode",""+funcclass.ACCESS_FULL);
			//return UserAccountsConsts?PG_APP_REDIRECT;
			String instant = (String) vS.getAttribute("instant");
			if (instant != null) {
				if (instant.equals("true")) {
					instant = "?instant=true";
				} else
					instant = "";
			} else
				instant = "";

			vS.removeAttribute("instant");

            DoctorLogin doctorLogin = new DoctorLogin();
            doctorLogin.setDoctorId(dnum);
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
            DoctorService.insertDoctorLogin(doctorLogin);

            int loginCountForLastVersion = DoctorService.getLoginCountForLastVersion(dnum);
            vS.setAttribute("lastVersionLoginCount", loginCountForLastVersion);

			String sharedPatientParameter = "";
			if (!StringUtils.isEmpty(sharedPatientNumber)) {
				logger.info("Doctor will be redirected to shared patient {}", sharedPatientNumber);
				sharedPatientParameter = (StringUtils.isEmpty(instant) ? "?": "&") + "sharedPatientNumber=" + sharedPatientNumber;
			}

			if (funcclass.isQaEnvironment() || funcclass.isProdEnvironment()) {
				Map<String, String> parameters = new HashMap<>();
				parameters.put(ZohoCrmClient.LAST_CLOUD_LOGIN, new Date().toString());
//				ZohoCrmClient.updateAccountsRecord(parameters, dnum);
				ZohoCrmClient.updateAccountsRecord(parameters, 4);
			}
			//***remove check - add checkbox on registration page (by clicking on register you agree to the terms stipulated in this link)
			//            if (!UserAccounts.getDBConnection().isViewedDisclaimer(dnum))
			//            {
			//                return "disclaimer.jsp" + instant;
			//            }
			//return "manager.jsp" + instant;

			return "cephx.jsp" + instant + sharedPatientParameter;
		}
		if (vLoginType == funcclass.ADMIN) {
			SessionUtility.setAdmin(vS); // admin security
			//vS.setAttribute(UserAccountsConsts.PRM_LOCATION,"_admin/Admin_cephX.jsp");
			//return UserAccountsConsts.PG_APP_REDIRECT;
			logger.info("Redirecting to admin page");
			return "_admin/Admin_cephX.jsp";
		}
		if (vLoginType == funcclass.GUEST || vLoginType == funcclass.SHARE_ALL_R || vLoginType == funcclass.SHARE_ALL_RW || vLoginType == funcclass.SHARE_ONE_R || vLoginType == funcclass.SHARE_ONE_RW) {
			if (vLoginType == funcclass.GUEST)
				cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_MULTI);
			else if (vLoginType == funcclass.SHARE_ALL_R)
				cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_MULTI);
			else if (vLoginType == funcclass.SHARE_ALL_RW)
				cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL_MULTI);
			else if (vLoginType == funcclass.SHARE_ONE_R)
				cs.put(cs.ACCESS, "" + funcclass.ACCESS_READ_ONLY_SINGLE);
			else if (vLoginType == funcclass.SHARE_ONE_RW)
				cs.put(cs.ACCESS, "" + funcclass.ACCESS_FULL_SINGLE);

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

	private static BasicDoc createNewDoctorInfo(HttpServletRequest vReq) {
		BasicDoc bd = new BasicDoc();
		bd.name = vReq.getParameter("fname");
		bd.lastname = vReq.getParameter("lname");
		bd.email = vReq.getParameter("email");
		bd.user = bd.email;
		bd.password = vReq.getParameter("password");
		bd.address = vReq.getParameter("address");
		bd.address2 = vReq.getParameter("address2");
		bd.city = vReq.getParameter("city");
		bd.state = vReq.getParameter("state");
		bd.country = vReq.getParameter("country");
		bd.phoneNumber = vReq.getParameter("phone");
		bd.fax = vReq.getParameter("fax");
		bd.zip = vReq.getParameter("zip");
		bd.partnerId = vReq.getParameter("partner_id") == null || !StringUtils.isNumeric(vReq.getParameter("partner_id").trim()) ? -1 : Long.parseLong(vReq.getParameter("partner_id").trim());
		bd.userType = new Integer(vReq.getParameter("utype")).intValue();
		bd.authorized = true;
		bd.isOrto2 = Boolean.valueOf(vReq.getParameter("isOrto2")).booleanValue();
		bd.extDoctorId = vReq.getParameter("ext_doctor_id") != null ? vReq.getParameter("ext_doctor_id") : "";
		if (bd.extDoctorId.isEmpty()) {
			bd.extDoctorId = vReq.getParameter("ext_doc_ID") != null ? vReq.getParameter("ext_doc_ID") : "";
		}

		if(vReq.getParameter("cost_per_case") != null){
			bd.costPerCase = Double.parseDouble(vReq.getParameter("cost_per_case"));
		}

		if(vReq.getParameter("monthly_fee") != null){
			bd.monthlyCost = Double.parseDouble(vReq.getParameter("monthly_fee"));
		}

		if (vReq.getParameter("credits") != null) {
			bd.freeAnalyses = Integer.parseInt(vReq.getParameter("credits"));
		} else {
			bd.freeAnalyses = null;
		}
		if(vReq.getParameter("doctor_id") != null){
			bd.docnum = Long.parseLong(vReq.getParameter("doctor_id"));
		}
		// default behavior 
		bd.isAlgoCeph = true;
		bd.allowedToTrace = false;
		bd.setScaleEstimation(true);


		return bd;
	}

	private static void clearSession(HttpSession vS) {
		java.util.Enumeration e = vS.getAttributeNames();
		while (e.hasMoreElements()) {
			System.out.println("ELEMS IN SESION $$$$$ " + (String) e.nextElement());
		}

	}

	public static void getDocsInExcel(HttpServletRequest vReq, HttpServletResponse vRes) {
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

	public static void getPDF(HttpServletRequest vReq, HttpServletResponse vRes) {
		vRes.setContentType("application/pdf");
		vRes.setHeader("Content-Disposition", "filename=test.pdf;");
		long patnum = Long.parseLong(vReq.getParameter("patnum"));

		try {
			new pdf(patnum, null, new CountingOutputStream(vRes.getOutputStream()), "", "", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void getSmartPDF(HttpServletRequest vReq, HttpServletResponse vRes) {
		long patientNumber = Long.parseLong(vReq.getParameter("patnum"));
		long doctorNumber = Long.parseLong(vReq.getParameter("docnum"));
		String patientName = vReq.getParameter("patname");
		String docName = vReq.getParameter("DocName");
		String showLogo = "true";

		logger.info("Downloading analysis for patient {}", patientNumber);

		if (vReq.getParameter("showLogo") != null) {
			showLogo = vReq.getParameter("showLogo");
			logger.info("showLogo param string is " + showLogo + " ,after boolean parsing is " + new Boolean(showLogo));
		}

		vRes.setContentType("application/pdf");
		String fileName = getDownFileName(patientName, patientNumber) + ".pdf";

		//vRes.setHeader("content-disposition", "inline;filename=" + fileName);
		vRes.setHeader("content-disposition", "attachment;filename=" + fileName);
		logger.info("Filename of analysis in PDF format is {}", fileName);

		StringTokenizer st = new StringTokenizer(vReq.getParameter("data"), UserAccountsConsts.DEF_DELIM);
		String[] ret = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			ret[i++] = st.nextToken();
		}
		DBconnection cn = UserAccounts.getDBConnection();

		try {
			CountingOutputStream cos = new CountingOutputStream(vRes.getOutputStream());
			new pdf(patientNumber, ret, cos, docName, cn.getDocPreferences(doctorNumber).getLogoPath(), new Boolean(showLogo).booleanValue());

			system.log("number of bytes are: " + cos.getByteCount());
			vRes.setContentLength((int) cos.getByteCount());


		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void getEcephPDF(HttpServletRequest vReq, HttpServletResponse vRes) {

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

		if (compnum > 0)
			logo_doc_num = compnum;
		else
			logo_doc_num = docnum;

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

	public static void getCastomAsImage(HttpServletRequest vReq, HttpServletResponse vRes) throws Exception {
		int w = Integer.parseInt(vReq.getParameter("w"));
		int h = Integer.parseInt(vReq.getParameter("h"));


		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();

		String SpecialAnalId = "***" + vReq.getParameter("annum");
		long patnum = Long.parseLong(vReq.getParameter("patnum"));
		String docName = "";
		long docnum = DoctorService.getDoctorNumberFromUser("" + patnum);
		boolean showLogo = true;

		SpecialAnalysisJPG sa = new SpecialAnalysisJPG(patnum, new String[]{SpecialAnalId}, docName, UserAccounts.getDBConnection().getDocPreferences(docnum).getLogoPath(), new Boolean(showLogo).booleanValue(), w, h);
		img = sa.createPdf("");
		system.log("got special analysis jpg");

		vRes.setContentType("image/jpeg");
		try {

			OutputStream out = vRes.getOutputStream();
			//File f = new File(funcclass.baseDir+"\\"+"testmyImg.jpg");
			//FileOutputStream out = new FileOutputStream(f);


			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);

			JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);

			param.setQuality(1.0f, false);

			encoder.encode(img, param);
			out.flush();
			out.close();
		} catch (Exception e) {
			System.out.println("Can't create Image from graphics ");
			e.printStackTrace();
		} finally {
			g2.dispose();
		}


	}

	public static void getImageTst(HttpServletRequest vReq, HttpServletResponse vRes) throws Exception {
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

	protected static BufferedImage draw(String st) {
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


	public static void getSmartPDF_print(HttpServletRequest vReq, HttpServletResponse vRes) {

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
			if (tmpLogo != null)
				showLogo = tmpLogo;

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


		system.log("<I style='color:red'>@@@@@@@@@@@@@@@@@@@@@@~  smart PDF Print " + vReq.isSecure() + "</I><BR>");
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

	public static void sendMailNewDoctor(BasicDoc docInfo) {


		String subject = "New Doctor";
		String message = "A new doctor has registered.\n Name:" + docInfo.name + " " + docInfo.lastname + "\n";
		String address = funcclass.defaultMailFrom;
		try {
			MailService.sendMail(message, address, subject, false);
		} catch (Exception e) {
			system.log("Can't send mail to Danny about new doctor");
			e.printStackTrace();
		}

		subject = "cephX.com registration";
		message = "Dear cephX user,\n\n" +
				" You have successfully completed the cephX registration.\n" +
				" As a new subscriber you are entitled to a free trial version,\n" +
				" for 1 patient record, including free cephalometric analysis and photo storage.\n" +
				" So start uploading your cephs and patient photos. You may trace the ceph yourself or" +
				"\n request your free tracing and analysis from our trained staff.\n" +
				" To start using cephx now, simply click on the following link: " +
				" " + funcclass.baseUrl_REG + funcclass.startPage + "\n" +

				"\n Thank you and welcome.\n\n" +
				" The cephX team.";
		address = docInfo.email;
		try {
			MailService.sendMail(message, address, subject, false);
		} catch (Exception e) {
			system.log("Can't send mail to Doctor about new account ... ");
			e.printStackTrace();
		}
	}


	public static void getExcel(HttpServletRequest vReq, HttpServletResponse vRes) {
		String PatName = "PatientAnalysis";

		if (vReq.getParameter("patname") != null)
			PatName = vReq.getParameter("patname");
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

	public static void getPatTxAttach(HttpServletRequest vReq, HttpServletResponse vRes) {
		long patnum = Long.parseLong(vReq.getParameter("patId"));
		String patName = vReq.getParameter("patName");

		TxAttach ta = new TxAttach(vReq.getParameter("patId"));
		File f = ta.getUserFile();
		if (ta == null) {
			system.log("file streamer download user TxAttachment was failed for patient " + patName + " [" + patnum + "]");
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
			system.log("File streamer download user TxAttachment was failed for patient " + patName + " [" + patnum + "] by the following reasons: \n");
			system.printStackTrace(e);
		}
	}

	private static boolean isHacker(String vUser, String vPass) {
		if (isBadCharacters(vUser) || isBadCharacters(vPass))
			return true;
		return false;
	}

	private static boolean isBadCharacters(String vStr) {
		if (vStr.indexOf("\"") != -1 || vStr.indexOf("'") != -1)
			return true;
		return false;
	}

	private static CephxSession setDocByType(int vType, String vUser, String vPass) {
		/*
        CephxSession cs = new CephxSession();
        cs.put(CephxSession.LOGIN_TYPE,""+vLoginType);
        cs.put(CephxSession.USER,""+vUser);
        cs.put(CephxSession.PASS,""+vPass);

        if(vType == funcclass.ADMIN)
            return cx;
        if(vType == funcclass.PATIENT)
        {
            BasicPerson pat=new BasicPerson();
            pat.number=Long.valueOf(vPass).longValue();
            cs.put(CephxSession.PAT_OBJ,pat);
            return cx;
        }

        Cdoctor docObj = new Cdoctor();

        if(vType==funcclass.DOCTOR || vType==funcclass.OPERATOR || vType==funcclass.GP || vType==funcclass.STUDENT || vType==funcclass.COMPANY)
        {
            long dnum;
            try
            {
                dnum = UserAccounts.getDBConnection().getDocNumFromUser(vUsr);
            }
            catch(Exception e)
            {
                return null;
            }
            UserAccounts.getDBConnection().incCounter("login",dnum);

        }


        if(vType==funcclass.GUEST  || vType==funcclass.SHARE_ALL_R || vType==funcclass.SHARE_ALL_RW || vType==funcclass.SHARE_ONE_R || vType==funcclass.SHARE_ONE_RW)
        {

        }
		 */
		return null;
	}

	private static String getDownFileName(String vName, long vNum) {

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

	public static void deletePatPic(HttpServletRequest vReq, HttpServletResponse vRes) {

		String strPatNum = vReq.getParameter("patnum");
		String strDocNum = vReq.getParameter("docnum");
		String strPicId = vReq.getParameter("picId");
		String strFileName = vReq.getParameter("fileName");

		long patnum = Long.parseLong(strPatNum);
		long docnum = Long.parseLong(strDocNum);
		long picId = Long.parseLong(strPicId);

		HttpSession session = vReq.getSession(true);
		try {
			if ((picId > 0) && (docnum > 0)) {
				PictureService.deletePicture(picId);

				File doctorDir = new File(funcclass.scannedFilePath + docnum + funcclass.FILE_DELIMETER);
				if (!doctorDir.exists())
					doctorDir.mkdir();
				//create patient directory
				File patientDir = new File(doctorDir.getAbsolutePath() + funcclass.FILE_DELIMETER + patnum);
				if (!patientDir.exists())
					patientDir.mkdir();
				//create ceph file	        
				File imageFile = new File(patientDir.getAbsolutePath() + funcclass.FILE_DELIMETER + strFileName);
				imageFile.deleteOnExit();

				SessionUtility.deleteFile(session, strPatNum, strFileName);

				//delete from s3
				funcclass.deleteObject(funcclass.getS3Key(patnum, docnum, imageFile.getName().split("\\.")[0]));

				//delete picture from file system
				//    	   copyCephFileToSessionDir(session,patientNum,rndFieName);
				//delete file from session temp directory

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void randomizeNewPassword(HttpServletRequest vReq, HttpServletResponse vRes) {
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


	public static void passwordRecovery(HttpServletRequest vReq, HttpServletResponse vRes) {
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
				sb.append("<div style='text-align:center'><img src='https://cloud.cephx.com/cephx/graphics/mailLogo.png' width=143 height=80  border=0> </div>");
				sb.append("<p><b>Hi "+ bd.getDocFullName() +",</b></p>");
				sb.append("<br></br>");
				sb.append("<br></br>");
				sb.append("<br></br>");
				sb.append("CephX recently received a request for a forgotten password.To change your CephX password, please click on this <a href='"+ funcclass.baseUrl + "passwordRecovery.jsp?hash=" + hash +"'>link</a>.If you did not request this change, you do not need to do anything.");
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
			if (writer != null)
				writer.close();
		}
	}

	public static void passwordRecoveryConfirm(HttpServletRequest vReq, HttpServletResponse vRes) {
		String hash = vReq.getParameter("hash");
		String password = vReq.getParameter("pass");
		vRes.setContentType("text/plain");

		DBconnection db = DBconnection.GetDBconnection();
		try {
			try {
				Long docId = db.getDocIdFromasswordRecovery(hash);
				if(docId == null)
					throw new Exception("Hash doesn't exists or expired");
				String passwordHash = PasswordHash.createHash(StringUtility.MD5(password));
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


    public static void uploadImageToServer(HttpServletRequest vReq, HttpServletResponse vRes) throws IOException {
		String img64 = vReq.getParameter("image");
		String patId = vReq.getParameter("patId");
		String docId = vReq.getParameter("docId");

		DBconnection db = DBconnection.GetDBconnection();
		String suffix = String.valueOf(new Date().getTime());

		File newFile = new File(funcclass.getPathForPatientOther(Long.valueOf(patId), Long.valueOf(docId), suffix));

		ImageConverter.base64ToFile(img64, newFile);

		funcclass.uploadFileToS3(Paths.get(newFile.getPath()), funcclass.gets3KeyPrefix(Long.valueOf(docId), Long.valueOf(patId)) + newFile.getName());

		db.insertNewPicture(FileUtility.getExtension(newFile.getName()),Long.valueOf(patId), Long.valueOf(docId), newFile.getName(), 3);

		if(newFile != null && newFile.exists())
			newFile.delete();
	}

	private static String getIp(final HttpServletRequest vReq) {
		final String xForwardedForHeader = vReq.getHeader("X-Forwarded-For");
		return xForwardedForHeader == null ? vReq.getRemoteAddr() : new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
	}
}
