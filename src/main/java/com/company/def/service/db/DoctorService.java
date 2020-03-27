package com.company.def.service.db;

import com.company.def.BasicDoc;
import com.company.def.CPatient;
import com.company.def.CephXScale;
import com.company.def.DBconnection;
import com.company.def.Trace;
import com.company.def.exceptions.NoSuchDoctorException;
import com.company.def.exceptions.NoSuchPatientException;
import com.company.def.funcclass;
import com.company.def.model.DoctorLogin;
import com.company.def.model.PatientImage;
import com.company.def.model.billing.BillingPlan;
import com.company.def.struct.struct.CephxFSModel;
import com.company.def.system;
import com.company.def.struct.struct.PictureEntry;
import com.company.def.util.date.DateUtility;
import com.company.def.util.file.FileUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class DoctorService {
    private static DBconnection connection = DBconnection.GetDBconnection();
    private final static Logger logger = LogManager.getLogger(DoctorService.class);

    public static long getDoctorNumberByPatient(long patientNumber) {
        long doctorNumber = -1L;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT doctor_number FROM analysis WHERE Serial_Number=" + patientNumber;
            rs = connection.execQuery(sql, statement);
            if (rs.next()) {
                doctorNumber = rs.getLong("doctor_number");
                logger.info("For patient {} doctor number is {}", patientNumber, doctorNumber);
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.error("Error during get doctor by patientId", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return doctorNumber;
    }

    public static void setDoctorBillingMethod(long doctorNumber, String billingMethod) {
        try {
            String sql = "UPDATE doctors SET billing_method =" +
                (billingMethod == null ? "NULL" : "'" + billingMethod + "'") + " WHERE doctor_number=" + doctorNumber;
            connection.execStatement(sql);
            logger.info("Biling method is updated to {} for doctor {}", billingMethod, doctorNumber);
        } catch (Exception e) {
            logger.error("Error during updating billing method for doctor " + doctorNumber, e);
        }
    }

    public static BasicDoc getDoctorInfoById(long doctorNumber) {
        BasicDoc docInfo = null;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM doctors WHERE doctor_number=" + doctorNumber;
            rs = connection.execQuery(sql, statement);
            if (rs.next()) {
                docInfo = getDoctorInfo(rs);
            } else {
                throw new NoSuchDoctorException(doctorNumber);
            }
        } catch (Exception e) {
            logger.error("Error during get doctor by id", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return docInfo;
    }

    public static BasicDoc getDoctorInfoByEmail(String email) {
        BasicDoc docInfo = null;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM doctors WHERE email='" + email + "'";
            rs = connection.execQuery(sql, statement);
            if (rs.next()) {
                docInfo = getDoctorInfo(rs);
            } else {
                throw new RuntimeException("Doctor with email " + email + " doesn't exist in the system.");
            }
        } catch (Exception e) {
            logger.error("Error during get doctor by email", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return docInfo;
    }

    public static String getDoctorLogoUrl(long doctorNumber) {
        String doctorLogoPath = null;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT icon FROM doc_preferences WHERE doc_id=" + doctorNumber;
            rs = connection.execQuery(sql, statement);
            if (rs.next()) {
                doctorLogoPath = rs.getString("icon");
                if (StringUtils.isEmpty(doctorLogoPath)) {
                    doctorLogoPath = null;
                } else {
                    doctorLogoPath = funcclass.DOC_LOGO_URL + doctorLogoPath;
                }
            }
        } catch (Exception e) {
            logger.error("Error during get doctor's logo", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return doctorLogoPath;
    }

    private static BasicDoc getDoctorInfo(ResultSet rs) throws SQLException {
        BasicDoc docInfo = new BasicDoc();
        docInfo.docnum = rs.getLong("doctor_number");
        docInfo.email = rs.getString("email");
        docInfo.lastname = rs.getString("last_name");
        docInfo.zip = rs.getString("zip");
        docInfo.country = rs.getString("country");
        docInfo.city = rs.getString("city");
        docInfo.name = rs.getString("doctor_name");
        docInfo.password = rs.getString("password");
        docInfo.sync = rs.getLong("sync");
        docInfo.authorized = rs.getBoolean("authorized");
        docInfo.address = rs.getString("address");
        docInfo.phoneNumber = rs.getString("telephone");
        docInfo.default_analysis = rs.getInt("default_analysis");
        docInfo.default_view = rs.getInt("default_view");
        docInfo.num_records_view = rs.getInt("num_records_view");
        docInfo.default_sort = rs.getInt("default_sort");
        docInfo.user = rs.getString("user");
        docInfo.userType = rs.getInt("userType");
        docInfo.allowedToTrace = rs.getBoolean("allowed_to_trace");
        docInfo.isLegit = rs.getBoolean("is_Legit");
        docInfo.alertOnSlack = rs.getBoolean("alert_on_slack");
        docInfo.isCompany = rs.getBoolean("is_company");
        docInfo.address2 = rs.getString("address2");
        docInfo.fax = rs.getString("fax");
        docInfo.state = rs.getString("state");
        docInfo.isAlgoCeph = rs.getBoolean("algo_ceph");
        docInfo.partnerId = rs.getLong("partner_id");
        docInfo.partners = connection.getPartnerNames();
        docInfo.freeAnalyses = rs.getInt("counter_credits");
        docInfo.costPerCase = rs.getDouble("cost_per_case");
        docInfo.thresholdLevel = rs.getInt("threshold_level");
        docInfo.costAfterThreshold = rs.getDouble("cost_after_threshold");
        docInfo.monthlyCost = rs.getDouble("monthly_cost");
        docInfo.bsSubscriptionId = rs.getInt("bs_subscription_id");
        docInfo.dateOfBs = rs.getDate("date_of_bs");
        docInfo.billingMethod = rs.getString("billing_method");
        docInfo.freeMonths = rs.getInt("free_months");
        docInfo.prepaidAnalyses = rs.getInt("prepaid_analysis");
        docInfo.setToOperatorAfterFailedAlgo(rs.getBoolean("to_operator_after_failed_algo"));
        docInfo.setLanguage(rs.getString("language"));
        docInfo.setTemplateEditorAccess(rs.getBoolean("templates_access"));
        docInfo.setMaxUploadedImagesCount(rs.getInt("max_uploaded_images"));
        docInfo.setScaleEstimation(rs.getBoolean("scale_estimation"));
        if (docInfo.partnerId != 0) {
            docInfo.partner = connection.getPartner(docInfo.partnerId);
        }
        try {
            docInfo.tStamp = rs.getDate("tStamp");
        } catch (Exception e) {

        }
        docInfo.isOrto2 = rs.getBoolean("isOrto2");
        docInfo.hasBillingPlan = ("bbb").equals(docInfo.billingMethod) && DBconnection.GetDBconnection().hasBillingPlan(docInfo.docnum);
        return docInfo;
    }

    public static boolean isFirstLogin(long doctorNumber) {
        boolean isFirst = true;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            String sql = "SELECT countVal from tblhitcounter WHERE counter_name = 'login' AND docnum=" + doctorNumber + " LIMIT 1";
            rs = connection.execQuery(sql, statement);
            isFirst = !rs.next();
        } catch (Exception e) {
            logger.warn("Error during get isFirstLogin for doctor " + doctorNumber, e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        logger.info("For doctor {} isFirstLogin = {}", doctorNumber, isFirst);
        return isFirst;
    }

    public static void deleteLogins(long doctorNumber) {
        try {
            String sql = "DELETE FROM tblhitcounter WHERE counter_name = 'login' AND docnum=" + doctorNumber;
            connection.execStatement(sql);
            logger.info("Loginings deleted successfully");
        } catch (Exception e) {
            logger.error("Can't delete loginings ", e);
        }
    }

    public static void updateDoctorLanguage(long doctorNumber, String language) {
        system.log("Updating language for doctor " + doctorNumber + " to " + language);
        try {
            String sql = "UPDATE doctors SET language ='" + language + "' WHERE doctor_number=" + doctorNumber;
            connection.execStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            system.log(e.getMessage());
        }
    }

    public static String getDoctorLanguage(String user) {
        String language = null;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT language FROM doctors where user='" + user + "'", statement);
            if (rs.next()) {
                language = rs.getString("language");
            }
        } catch (Exception e) {
            e.printStackTrace();
            system.log(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }

        return language;
    }

    public static boolean isDoctorAllowedForScaleEstimation(long doctorNumber) {
        boolean scaleEstimation = false;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT scale_estimation FROM doctors where doctor_number=" + doctorNumber, statement);
            if (rs.next()) {
                scaleEstimation = rs.getBoolean("scale_estimation");
                logger.info("Scale estimation for doctor {} is {}", doctorNumber, scaleEstimation);
            }
        } catch (Exception e) {
            logger.error("Can't get doctor scale estimation", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return scaleEstimation;
    }

    public static long getDoctorNumberFromUser(String user) {
        long doctorNumber = -1;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT doctor_number FROM doctors WHERE user='" + user + "'", statement);
            if (rs.next()) {
                doctorNumber = rs.getLong("doctor_number");
            }
        } catch (Exception e) {
            e.printStackTrace();
            system.log(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return doctorNumber;
    }

    public static String getDoctorEmail(long doctorNumber) {
        String email = null;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT email FROM doctors where doctor_number=" + doctorNumber, statement);
            if (rs.next()) {
                email = rs.getString("email");
            }
        } catch (Exception e) {
            e.printStackTrace();
            system.log(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return email;
    }

    public static void insertDemoPatientForDoctor(long doctorNumber, final List<PatientImage> demoPatientImages) throws Exception {
        CPatient newPat = connection.getSetedPatient(funcclass.DEMO_PAT_ID);
        newPat.AdvancedInfo.DocNum = doctorNumber;
        newPat.AdvancedInfo.archived = false;
        newPat.AdvancedInfo.isFree = true;
        final boolean sync = connection.getDocInfo(doctorNumber).hasBillingPlan;
        long patientNumber = connection.saveNewPat(newPat, 0, 0, sync);
        logger.info("Demo patient {} is created for doctor {}", patientNumber, doctorNumber);

        updateDicomFields(patientNumber);


        /*Copy general trace*/
        LandmarkService.insertLandmarks(patientNumber, newPat.GetAllPointsVector(),false);
        /*Copy save_as_list*/
        Vector demoTraces = connection.getTraces(funcclass.DEMO_PAT_ID);
        for (int i = 0; i < demoTraces.size(); ++i) {
            Trace t = (Trace) demoTraces.elementAt(i);
            t.setPatId(patientNumber);
            logger.info("going to insertNewTrace, trace name is: " + t.getName());
            connection.insertNewTrace(t);
        }

        /*copy photo_scale*/
        CephXScale sc = connection.getPhotoScale(funcclass.DEMO_PAT_ID);
        if (sc != null) {
            connection.insertPhotoScale(patientNumber, sc.getX1(), sc.getY1(), sc.getX2(), sc.getY2(), sc.getPhotRatio());
        }
        final String demoCephS3Path = PatientService.getCephPath(funcclass.DEMO_PAT_ID);
        for (PatientImage patientImage : demoPatientImages) {
            connection.copyPatientImageForPatient(patientImage, patientNumber);
        }

        final File demoCephFile = funcclass.getCephFileFromS3(funcclass.DEMO_PAT_ID, funcclass.DEMO_DOCTOR_ID);
        final CephxFSModel cmDEMO = new CephxFSModel(funcclass.DEMO_DOCTOR_ID, funcclass.DEMO_PAT_ID);
        final CephxFSModel cmNew = new CephxFSModel(doctorNumber, patientNumber);
        if (demoCephFile != null && demoCephFile.exists()) {
            funcclass.uploadCephFileTos3(demoCephFile.getAbsolutePath(), patientNumber, doctorNumber);
        } else {
            copyDemoImageFromTmpUploaded();

            FileInputStream demoCeph = cmDEMO.getCeph();
            byte[] ba = new byte[demoCeph.available()];
            demoCeph.read(ba);
            FileOutputStream newCeph = new FileOutputStream(cmNew.getPath() + cmNew.CEPH_DEF_NAME);
            newCeph.write(ba);
            demoCeph.close();
            newCeph.close();
            Path path = Paths.get(cmNew.getPath() + cmNew.CEPH_DEF_NAME);
            funcclass.uploadCephFileTos3(cmNew.getPath() + cmNew.CEPH_DEF_NAME, patientNumber, doctorNumber);
            logger.info("Ceph file for patient {} is uploaded successfully to s3 from path {}", patientNumber, path);
            path.toFile().delete();
        }
        try {
            connection.addPatPicture(FileUtility.getExtension(cmDEMO.getCurrentProfile()), patientNumber, doctorNumber, cmDEMO.getCurrentProfile(), funcclass.PIC_TYPE_PROFILE);
            final File profileFile = funcclass.getProfileFileFromS3(funcclass.DEMO_PAT_ID, funcclass.DEMO_DOCTOR_ID);
            funcclass.uploadFileToS3(profileFile.toPath(), funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE));
            if (profileFile.exists()) {
                profileFile.delete();
            }
        } catch (Exception e) {
            logger.error("Error during creating profile for patient " + patientNumber, e);
        }

        try {
            final Path path = Paths.get(cmNew.getPath() + "profile_pdf");
            final File profilePdfFile = funcclass.getFileFromS3(path, funcclass.getS3Key(funcclass.DEMO_PAT_ID, funcclass.DEMO_DOCTOR_ID, "profile_pdf"));
            funcclass.uploadFileToS3(profilePdfFile.toPath(), funcclass.getS3Key(patientNumber, doctorNumber, "profile_pdf"));
            if (profilePdfFile.exists()) {
                profilePdfFile.delete();
            }
        } catch (Exception e) {
            logger.error("Error during creating profile for patient " + patientNumber, e);
        }
    }

    private static void updateDicomFields(long patientNumber) throws Exception {
        PatientService.updateDicomStatus(patientNumber,false);
        PatientService.updateHasAirways(patientNumber,PatientService.hasAirwaysData(funcclass.DEMO_PAT_ID));
        PatientService.addCephS3Path(patientNumber,PatientService.getCephPath(funcclass.DEMO_PAT_ID));
        PatientService.addStl1S3Path(patientNumber,PatientService.getStlPath(funcclass.DEMO_PAT_ID));
        connection.setAnalysisJsonPathVerbatime(patientNumber,PatientService.stlJsonPath(funcclass.DEMO_PAT_ID));
        connection.setPatZipStl(patientNumber,PatientService.isZipStlExist(funcclass.DEMO_PAT_ID));
        connection.setPatZipVideo(patientNumber,PatientService.isZipVideoExist(funcclass.DEMO_PAT_ID));

    }

    private static void copyDemoImageFromTmpUploaded() {
        try {
            final String demoFileSubpath = funcclass.FILE_DELIMETER + funcclass.DEMO_DOCTOR_ID + funcclass.FILE_DELIMETER + funcclass.DEMO_PAT_ID + funcclass.FILE_DELIMETER + CephxFSModel.CEPH_DEF_NAME;
            final File demoFileFromTmpUploadedFolder = new File(funcclass.tmpuploaded + demoFileSubpath);
            final File demoFileInScannedCephFolder = new File(CephxFSModel.BASE + demoFileSubpath);
            if (demoFileFromTmpUploadedFolder.exists() && (!demoFileInScannedCephFolder.exists())) {
                demoFileInScannedCephFolder.getParentFile().mkdirs();
                FileUtils.copyFile(demoFileFromTmpUploadedFolder, demoFileInScannedCephFolder);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public static void insertDoctorLogin(DoctorLogin doctorLogin) {
        String sql = "INSERT INTO doctor_logins (doctor_id, cephx_version, ip) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, doctorLogin.getDoctorId());
            preparedStatement.setString(2, doctorLogin.getCephxVersion());
            preparedStatement.setString(3, doctorLogin.getIp());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.error("Can't insert doctor login", e);
        }
    }

    public static int getLoginCountForVersion(long doctorId, String cephxVersion) {
        int loginCount = 0;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT COUNT(*) FROM doctor_logins WHERE doctor_id = " + doctorId + " AND cephx_version = '" + cephxVersion + "'", statement);
            if (rs.next()) {
                loginCount = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            system.log(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return loginCount;
    }

    public static int getLoginCountForLastVersion(long doctorId) {
        return getLoginCountForVersion(doctorId, funcclass.versionNumber);
    }

    public static int getDoctorsCount() {
        int count = 0;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT COUNT(*) FROM doctors", statement);
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.error("Can't get doctor's count", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return count;
    }

    public static boolean isDicomUploadAllowed(final long doctorId) throws Exception {
        boolean isAllowed = true;
        if (connection.getDocInfo(doctorId).hasBillingPlan) {
//            if (billingPlanExpired(doctorId)) {
//                return false;
//            }
            final BillingPlan billingPlan = connection.getBillingPlanByDoctorId(String.valueOf(doctorId));
            final List<String> billingPlanFeatures = connection.getFeaturesNames(doctorId);
            if (billingPlanFeatures.contains("3D cases")) {
                final Date monthStartDate = getBillingPlanMonthStartDate(doctorId);
                final long numberOfMonthUploads = connection.getCasesCountFromDate(doctorId, "3D", new java.sql.Date(monthStartDate.getTime()));
                isAllowed = numberOfMonthUploads < billingPlan.getCaseCount();
            } else {
                final long totalCasesCount = connection.getTotalCasesCount((int) doctorId, "3D");
                if (totalCasesCount > 0) {
                    return false;
                }
            }
        }
        return isAllowed;
    }

    public static boolean isCephUploadAllowed(final long doctorId) throws Exception {
        boolean isAllowed = true;
        if (connection.getDocInfo(doctorId).hasBillingPlan) {
//            if (billingPlanExpired(doctorId)) {
//                return false;
//            }
            final BillingPlan billingPlan = connection.getBillingPlanByDoctorId(String.valueOf(doctorId));
            final Date monthStartDate = getBillingPlanMonthStartDate(doctorId);
            final long numberOfMonthUploads = connection.getCasesCountFromDate((int) doctorId, "2D", new java.sql.Date(monthStartDate.getTime()));
            final List<String> billingPlanFeatures = connection.getFeaturesNames(doctorId);
            if (billingPlanFeatures.contains("2D cases")) {
                isAllowed = numberOfMonthUploads < billingPlan.getCaseCount();
            } else {
                isAllowed = numberOfMonthUploads < 1;
            }

        }
        return isAllowed;
    }

    public static Date getBillingPlanMonthStartDate(final long doctorId) {
        final Date planStartDate = connection.getBillingPlanDateByDoctorId(doctorId);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(planStartDate);
        final Date currentDate = new Date();
        while (calendar.getTime().getTime() < currentDate.getTime()) {
            calendar.add(Calendar.MONTH, 1);
        }
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    private static boolean billingPlanExpired(long doctorId) {
        final Date planStartDate = connection.getBillingPlanDateByDoctorId(doctorId);
        final String planType = connection.getBillingPlanTypeByDoctorId(doctorId);
        final Calendar cal = Calendar.getInstance();
        cal.setTime(planStartDate);
        if (planType.toUpperCase().equals("MONTHLY")) {
            cal.add(Calendar.MONTH, 1);
        }
        if (planType.toUpperCase().equals("ANNUAL")) {
            cal.add(Calendar.YEAR, 1);
        }
        return cal.getTime().before(new Date());
    }


    public static boolean isCaseSharingAllowed(final long doctorId) throws Exception {
        return connection.getFeaturesNames(doctorId).contains("Sharing");
    }

    public static boolean isProfileAllowed(final long doctorId) throws Exception {
        return connection.getFeaturesNames(doctorId).contains("Profile images");
    }

    public static boolean isImagegalleryAllowed(final long doctorId) throws Exception {
        return connection.getFeaturesNames(doctorId).contains("Imaging gallery");
    }

    public static boolean isSuperimpositionAllowed(final long doctorId) throws Exception {
        return connection.getFeaturesNames(doctorId).contains("Superimposition");
    }

    public static boolean isCbIdExists(long docNum) {
        return connection.isCbIdExists(docNum);
    }

    public static void setDoctorLegit(final Long doctorId, final boolean isLegit) {
        try {
            String sql = "UPDATE doctors SET is_Legit ='" +
                isLegit + "' WHERE doctor_number=" + doctorId;
            connection.execStatement(sql);
            logger.info("is_Legit is updated to {} for doctor {}", isLegit, doctorId);
        } catch (Exception e) {
            logger.error("Error during updating is_Legit for doctor " + doctorId, e);
        }
    }
}
