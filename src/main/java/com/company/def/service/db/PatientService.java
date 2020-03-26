package com.cephx.def.service.db;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.cephx.def.BasicDoc;
import com.cephx.def.BasicPatientInfoData;
import com.cephx.def.CAdvancedPatInfo;
import com.cephx.def.CBasicPatientInfo;
import com.cephx.def.CPatient;
import com.cephx.def.CephxParameters;
import com.cephx.def.DBconnection;
import com.cephx.def.PatientAnalysis;
import com.cephx.def.PatientPoints;
import com.cephx.def.SharedPatient;
import com.cephx.def.algoceph.AlgoCephClient;
import com.cephx.def.exceptions.NoSuchPatientException;
import com.cephx.def.funcclass;
import com.cephx.def.model.airways.AirwaysReportDataManager;
import com.cephx.def.servlets.admin.Partner;
import com.cephx.def.servlets.patient.AutoTraceData;
import com.cephx.def.servlets.patient.CoordinatesData;
import com.cephx.def.system;
import com.cephx.def.util.date.DateUtility;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class PatientService {
    private static DBconnection connection = DBconnection.GetDBconnection();
    private final static Logger logger = LogManager.getLogger(PatientService.class);

    /********************************** SHARED PATIENTS **********************************/

    public static void addSharedPatient(SharedPatient sharedPatient) throws Exception {
        logger.info("Inserting new sharing for patient {} ....", sharedPatient.getPatientNumber());
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder
                .append("INSERT INTO shared_patients(doctor_number, shared_with_email, shared_with_name, patient_number, shared_at, shared_with_number) VALUES (")
                .append(sharedPatient.getDoctorNumber()).append(",'")
                .append(sharedPatient.getSharedWithEmail()).append("','")
                .append(sharedPatient.getSharedWithName()).append("',")
                .append(sharedPatient.getPatientNumber()).append(", '")
                .append(DateUtility.getDateStringFormat(sharedPatient.getSharedAt(), "yyyy-MM-dd HH:mm:ss")).append("', ")
                .append(sharedPatient.getSharedWithNumber()).append(")");
        connection.execStatement(sqlBuilder.toString());
    }

    public static void unsharePatient(long patientNumber, long doctorNumber, String sharedWithEmail) {
        logger.info("Deleting sharing for patient {} .....", patientNumber);
        String sql = "DELETE FROM shared_patients WHERE doctor_number =" + doctorNumber +
                " AND patient_number =" + patientNumber + " AND shared_with_email ='" + sharedWithEmail + "'";
        try {
            connection.execStatement(sql);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static void unsharePatient(long patientNumber, long doctorNumber, long sharedWithNumber) {
        logger.info("Deleting sharing for patient {} ....", patientNumber);
        String sql = "DELETE FROM shared_patients WHERE doctor_number =" + doctorNumber +
                " AND patient_number =" + patientNumber + " AND shared_with_number =" + sharedWithNumber;
        try {
            connection.execStatement(sql);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public static List<CPatient> getSharedWithDoctorPatients(String doctorEmail) {
        String sql = "SELECT *  FROM analysis a INNER JOIN shared_patients s" +
                " ON a.Serial_Number = s.patient_number AND s.shared_with_email ='" + doctorEmail + "'";
        return getPatients(sql);
    }

    public static List<CPatient> getDoctorPatients(long doctorNumber) {
        String sql = "SELECT * FROM analysis WHERE doctor_number =" + doctorNumber;
        return getPatients(sql);
    }

    public static long getSharedWithDoctorPatientsCount(String doctorEmail) {
        long count = 0;
        Statement statement = connection.getStatement();
        String sqlBuilder = "SELECT COUNT(*)  FROM analysis a INNER JOIN shared_patients s" +
                " ON a.Serial_Number = s.patient_number AND s.shared_with_email ='" + doctorEmail + "'";
        ResultSet rs = connection.execQuery(sqlBuilder);
        try {
            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (Exception e) {
            logger.error("Unable to get shared with doctor patients count", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return count;
    }

    public static List<SharedPatient> getSharingsOfPatient(long patientNumber) {
        String sql = "SELECT a.*, s.shared_with_email as shared_email, s.shared_with_name as doc_name, " +
                "s.shared_at as shared_date, s.id as shared_id, s.shared_with_number as shared_with_number " +
                "FROM analysis a INNER JOIN shared_patients s" +
                " ON a.Serial_Number = s.patient_number AND s.patient_number =" + patientNumber;
        return getSharedPatients(sql);
    }

    private static List<SharedPatient> getSharedPatients(String sql) {
        Statement statement = connection.getStatement();
        ResultSet rs = connection.execQuery(sql);
        List<SharedPatient> patientList = new ArrayList<>();
        try {
            while (rs.next()) {
                SharedPatient sharedPatient = new SharedPatient();
                CAdvancedPatInfo advancedPatInfo =  new CAdvancedPatInfo();
                CBasicPatientInfo basicPatientInfo = new CBasicPatientInfo();
                connection.getPatientData(rs, advancedPatInfo, basicPatientInfo);

                basicPatientInfo.setDate(DateUtility.getCephDate(basicPatientInfo.getDate(), "-"));

                sharedPatient.setPatient(new CPatient(advancedPatInfo, basicPatientInfo, new PatientPoints()));

                sharedPatient.setId(rs.getLong("shared_id"));
                sharedPatient.setSharedAt(rs.getDate("shared_date"));
                sharedPatient.setSharedWithEmail(rs.getString("shared_email"));
                sharedPatient.setSharedWithName(rs.getString("doc_name"));
                sharedPatient.setSharedWithNumber(rs.getLong("shared_with_number"));
                patientList.add(sharedPatient);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return patientList;
    }

    public static void updateSharedWithDoctor(String email, long doctorNumber, String doctorName) {
        logger.info("Updating shared with doctor with email {} .", email);
        String sql = "UPDATE shared_patients SET shared_with_number =" + doctorNumber + ", shared_with_name = '" + doctorName +
                "' WHERE shared_with_email = '" + email + "'";
        try {
            connection.execStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
    }

    public static boolean isPatientSharedWithDoctor(long patientNumber, long doctorNumber) {
        boolean isPatientShared = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT * FROM shared_patients WHERE shared_with_number =" + doctorNumber
                + " AND patient_number = " + patientNumber;
        try {
            rs = connection.execQuery(sql);
            isPatientShared = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return isPatientShared;
    }


    /********************************** PATIENTS **********************************/

    private static List<CPatient> getPatients(String sql) {
        Statement statement = connection.getStatement();
        ResultSet rs = connection.execQuery(sql);
        List<CPatient> patientList = new ArrayList<>();
        try {
            while (rs.next()) {
                CAdvancedPatInfo advancedPatInfo = new CAdvancedPatInfo();
                CBasicPatientInfo basicPatientInfo = new CBasicPatientInfo();
                connection.getPatientData(rs, advancedPatInfo, basicPatientInfo);
                patientList.add(new CPatient(advancedPatInfo, basicPatientInfo, new PatientPoints()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return patientList;
    }

    public static CPatient getPatientWithoutPoints(long patientNumber) {
        CPatient patient = null;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
           String sql =  "SELECT * FROM analysis WHERE Serial_Number=" + patientNumber;
           rs = connection.execQuery(sql);
           if (rs.next()) {
               patient = new CPatient(getPatientAdvancedInfo(rs), getPatientBasicInfo(rs));
           }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return patient;
    }

    public static Hashtable searchPatients(String sql) {
        Hashtable recordTable = new Hashtable(2);
        Vector V = new Vector();
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            while (rs.next()) {
                CPatient patient = new CPatient(getPatientAdvancedInfo(rs), getPatientBasicInfo(rs), new PatientPoints());
                V.addElement(patient);
            }
        } catch (Exception e) {
            logger.error("Can't search patients", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        recordTable.put("vector", V);
        recordTable.put("total", V.size());
        return recordTable;
    }

    private static CBasicPatientInfo getPatientBasicInfo(ResultSet rs) {
        CBasicPatientInfo basicInfo = new CBasicPatientInfo();
        try {
            basicInfo.setNumber(rs.getLong("Serial_Number"));
            basicInfo.setFirstName(rs.getString("patient_first_name"));
            basicInfo.setLastName(rs.getString("patient_last_name"));
            try {
                basicInfo.setDate(funcclass.dbDateToRepDate(rs.getString("date_of_analysis")));
            } catch (Exception e) {
                logger.info("bad date format giving old date");
                basicInfo.setDate("01-01-2000");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return basicInfo;
    }


    private static CAdvancedPatInfo getPatientAdvancedInfo(ResultSet rs) {
        CAdvancedPatInfo advancedInfo = new CAdvancedPatInfo();
        try {
            advancedInfo.addType = rs.getString("add_type");
            advancedInfo.pat_n = rs.getInt("n");
            advancedInfo.pat_Gn = rs.getInt("gn");
            advancedInfo.pat_Curv = rs.getInt("curv");
            advancedInfo.pat_Discrepancy = rs.getInt("crowd");
            advancedInfo.pat_age = rs.getInt("age");
            advancedInfo.dateOfBirth = rs.getDate("birthday");
            advancedInfo.cephDate = rs.getDate("cephDate");
            advancedInfo.pat_company = rs.getLong("Company_Number");
            advancedInfo.pat_sex = rs.getInt("sex");
            advancedInfo.Jpg = rs.getString("file_name");
            advancedInfo.DocNum = rs.getLong("doctor_number");
            advancedInfo.owner = rs.getInt("owner");
            advancedInfo.scale = rs.getDouble("scale");
            if (advancedInfo.scale == 0) {
                advancedInfo.scale = 1;
            }
            advancedInfo.commentPhase = rs.getString("comment");
            advancedInfo.mainAnalysisId = rs.getLong("main_analysis_id");
            advancedInfo.phase = CAdvancedPatInfo.Phases.valueOf(rs.getString("phase"));
            advancedInfo.cupat = rs.getBoolean("cupat_cholim");
            advancedInfo.uploaded = rs.getInt("uploaded");
            advancedInfo.product = rs.getInt("product_number");
            advancedInfo.employeeNum = rs.getInt("Operator_Number");

            try {
                advancedInfo.time = rs.getString("time_of_analysis");
            } catch (Exception e) {
                advancedInfo.time = "";
            }
            advancedInfo.race = rs.getInt("race");
            advancedInfo.deleted = rs.getBoolean("deleted");
            advancedInfo.failedAlgo = rs.getBoolean("algo_failed");
            advancedInfo.traced = rs.getBoolean("trace");
            advancedInfo.archived = rs.getBoolean("archived");
            advancedInfo.isDigitized = rs.getBoolean("digitized");
            advancedInfo.SupervisorId = rs.getLong("SupID");
            advancedInfo.SupervisionStat = rs.getInt("SupStat");

            advancedInfo.pat_email = rs.getString("email");

            advancedInfo.algoCephHotPotato = rs.getBoolean("algo_ceph_hot_potato");
            if (advancedInfo.algoCephHotPotato) {
                advancedInfo.algoCephScaleCoords = new ArrayList<>();
                advancedInfo.algoCephScaleCoords.add(
                        CoordinatesData.instance(
                                rs.getString("algo_scale_x1"),
                                rs.getString("algo_scale_y1")));
                advancedInfo.algoCephScaleCoords.add(
                        CoordinatesData.instance(
                                rs.getString("algo_scale_x2"),
                                rs.getString("algo_scale_y2")));
            }
            advancedInfo.algoCephQuality = rs.getBigDecimal("algo_ceph_quality");
            advancedInfo.isAlgoCeph = advancedInfo.algoCephQuality != null;
            BigDecimal algoScaleConf = rs.getBigDecimal("algo_scale_confidence");
            if (algoScaleConf != null) {
                advancedInfo.algoScaleConfidence = algoScaleConf.setScale(2, RoundingMode.HALF_EVEN);
            }

            advancedInfo.ext_pat_id = rs.getString("ext_pat_id");
            advancedInfo.setEstimatedScale(rs.getBoolean("estimated_scale"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return advancedInfo;
    }



    public static void updatePatientInfo(CPatient patient) {
        String sql = "UPDATE analysis SET patient_first_name = ?, patient_last_name = ?, Company_Number = ?, " +
                "Operator_Number = ?, cupat_cholim = ?, product_number = ?, doctor_number = ?, scale = ?, owner = ?, " +
                "n = ?, gn = ?, crowd = ?, curv = ?, age = ?, sex = ?, birthday = ?, cephDate = ?, race = ?, " +
                "digitized = ?, SupID = ?, SupStat = ?, email = ?, phase = ?, comment = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, patient.getFirstName());
            preparedStatement.setString(2, patient.getLastName());
            preparedStatement.setLong(3, patient.AdvancedInfo.pat_company);
            preparedStatement.setLong(4, patient.AdvancedInfo.employeeNum);
            preparedStatement.setString(5, String.valueOf(patient.AdvancedInfo.cupat));
            preparedStatement.setInt(6, patient.AdvancedInfo.product);
            preparedStatement.setLong(7, patient.AdvancedInfo.DocNum);
            preparedStatement.setDouble(8, patient.AdvancedInfo.scale);
            preparedStatement.setInt(9, patient.AdvancedInfo.owner);
            preparedStatement.setInt(10, patient.AdvancedInfo.pat_n);
            preparedStatement.setInt(11, patient.AdvancedInfo.pat_Gn);
            preparedStatement.setInt(12, patient.AdvancedInfo.pat_Discrepancy);
            preparedStatement.setInt(13, patient.AdvancedInfo.pat_Curv);
            preparedStatement.setInt(14, patient.AdvancedInfo.pat_age);
            preparedStatement.setInt(15, patient.AdvancedInfo.pat_sex);
            preparedStatement.setDate(16, DateUtility.toSqlDate(patient.AdvancedInfo.dateOfBirth));
            preparedStatement.setDate(17, DateUtility.toSqlDate(patient.AdvancedInfo.cephDate));
            preparedStatement.setInt(18, patient.AdvancedInfo.race);
            preparedStatement.setString(19, String.valueOf(patient.AdvancedInfo.isDigitized));
            preparedStatement.setLong(20, patient.AdvancedInfo.SupervisorId);
            preparedStatement.setInt(21, patient.AdvancedInfo.SupervisionStat);
            preparedStatement.setString(22, patient.AdvancedInfo.pat_email);
            preparedStatement.setString(23, patient.AdvancedInfo.phase.getValue());
            preparedStatement.setString(24, patient.AdvancedInfo.commentPhase);
            preparedStatement.setLong(25, patient.BasicInfo.getNumber());
            preparedStatement.executeUpdate();
            logger.info("PatientInfo is successfully updated for patient {}", patient.BasicInfo.getNumber());
        } catch (Exception e) {
            logger.error("Can't update patientInfo of patient " + patient.BasicInfo.getNumber(), e);
        }
    }

    public static void insertPatient(CPatient patient) {

    }

    public static void updatePatientOperator(long patientNumber, long operatorNumber) {
        logger.info("Updating operator of patient " + patientNumber + " ......");
        try {
            String sql = "UPDATE analysis SET Operator_Number=" + operatorNumber + " WHERE Serial_Number=" + patientNumber;
            connection.execStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
    }

    public static int getNotDeletedPatientCount(long doctorNumber) {
        int count = 0;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT COUNT(*) FROM analysis WHERE deleted = 'false' AND doctor_number =" + doctorNumber + " and patient_first_name NOT like ('new demo') and patient_first_name NOT like ('Demo Patient')";
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.info("Can't count patients", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return count;
    }

    public static void updatePatientOwner(long patientNumber, int owner) {
        try {
            String sql = "UPDATE analysis SET owner=" + owner + " WHERE Serial_Number=" + patientNumber;
            connection.execStatement(sql);
            logger.info("Owner of patient {} is successfully updated to {}", patientNumber, owner);
        } catch (Exception e) {
            logger.info("Can't update owner of patient " + patientNumber, e);
        }
    }

    public static void updateEstimatedScale(long patientNumber, boolean estimatedScale) {
        try {
            String sql = "UPDATE analysis SET estimated_scale=" + estimatedScale + " WHERE Serial_Number=" + patientNumber;
            connection.execStatement(sql);
            logger.info("Estimated scale of patient {} is successfully updated to {}", patientNumber, estimatedScale);
        } catch (Exception e) {
            logger.info("Can't update estimated scale of patient " + patientNumber, e);
        }
    }

    public static boolean isEstimatedScale(long patientNumber) {
        boolean estimatedScale = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT estimated_scale FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                estimatedScale = rs.getBoolean("estimated_scale");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't check estimated scale", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return estimatedScale;
    }

    public static int getPatientOwner(long patientNumber) {
        int owner = 0;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT owner FROM analysis WHERE Serial_Number =" + patientNumber;
            rs = connection.execQuery(sql);
            if (rs.next()) {
                owner = rs.getInt("owner");
            }
        } catch (Exception e) {
            system.printStackTrace(e);
            logger.info(e.getMessage());
        } finally {
           DBconnection.closeResources(rs, statement);
        }
        return owner;
    }

    public static double getPatientScale(long patientNumber) {
        double scale = -100;
        logger.info("Getting scale of patient " + patientNumber + " ......");
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT scale FROM analysis WHERE Serial_Number=" + patientNumber;
            rs = connection.execQuery(sql);
            if (rs.next()) {
                scale = rs.getDouble("scale");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        } finally {
           DBconnection.closeResources(rs, statement);
        }
        return scale;
    }

    public static String getPatientName(long patientNumber) {
        String patientName = null;
        logger.info("Getting name of patient " + patientNumber + " ......");
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            String sql = "SELECT patient_first_name, patient_last_name FROM analysis WHERE Serial_Number=" + patientNumber;
            rs = connection.execQuery(sql);
            if (rs.next()) {
                String firstName = rs.getString("patient_first_name");
                String lastName = rs.getString("patient_last_name");
                patientName = firstName + (StringUtils.isEmpty(lastName) ? "" : " " + lastName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return patientName;
    }

    public static void setDigitized(long patientNumber, boolean isDigitized) {
        logger.info("Setting digitized parameter of patient " + patientNumber + " to " + isDigitized);
        try {
            String sql = "UPDATE analysis SET digitized='" + isDigitized + "' WHERE Serial_Number=" + patientNumber;
            connection.execStatement(sql);
        } catch (Exception e) {
            logger.info("Can't update digitized parameter of patient " + patientNumber, e);
        }
    }

    public static boolean isDigitizedPatient(long patientNumber) {
        boolean isDigitized = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT digitized FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                isDigitized = rs.getBoolean("digitized");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return isDigitized;
    }

    /**
     * Updating operator number for patient for determining who traced patient
     * @param patientNumber Number of traced patient
     * @param operatorNumber Number of operator (doctor who traced patient)
     */
    public static void updateOperatorNumber(long patientNumber, long operatorNumber) {
        logger.info("Updating operator number to " + operatorNumber  + " of patient " + patientNumber);
        String sql = "UPDATE analysis SET Operator_Number = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, operatorNumber);
            preparedStatement.setLong(2, patientNumber);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.info("Can't update operator number of patient " + patientNumber, e);
        }
    }

    public static void updateAlgoQualityForAnalysis(long patientNumber, BigDecimal algoTraceQuality) {
        logger.info("Updating algo_ceph_quality to " + algoTraceQuality  + " of patient " + patientNumber);
        String sql = "UPDATE analysis SET algo_ceph_quality = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setBigDecimal(1, algoTraceQuality);
            preparedStatement.setLong(2, patientNumber);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.info("Can't update algo_ceph_quality of patient " + patientNumber, e);
        }
    }

    public static boolean isAlgoCephHotPotato(long patientNumber) {
        boolean algoCephHotPotato = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT algo_ceph_hot_potato FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                algoCephHotPotato = rs.getBoolean("algo_ceph_hot_potato");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't get isAlgoCephHotPotato for patient " + patientNumber, e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return algoCephHotPotato;
    }

    public static boolean isAlgoCeph(long patientNumber) {
        boolean algoCeph = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT is_by_algoceph FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                algoCeph = rs.getBoolean("is_by_algoceph");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't get isAlgoCephHotPotato for patient " + patientNumber, e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return algoCeph;
    }

    public static void updateTraceTime(long patientNumber) {
        String sql = "UPDATE analysis SET opTraceDate = now() WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, patientNumber);
            preparedStatement.executeUpdate();
            logger.info("Trace time is updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update trace time for patient " + patientNumber, e);
        }
    }
    public static void updateTraced(long patientNumber, boolean isTraced){

        String sql = "UPDATE analysis SET trace = "+ (isTraced? 1 : 0) +" WHERE Serial_Number = "+patientNumber;
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Trace updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update trace for patient " + patientNumber, e);
        }

    }
    public static void updateDicomStatus(long patientNumber, boolean isDicom){

        String sql = "UPDATE analysis SET is_dicom = "+ (isDicom? 1 : 0) +" WHERE Serial_Number = "+patientNumber;
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Trace updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update trace for patient " + patientNumber, e);
        }
    }

    public static void updateComplitedStatus(long transactionId){

        String sql = "UPDATE dicom_tasks SET complited = 0 WHERE transaction_id = "+transactionId;
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.executeUpdate(sql);
            logger.info("complited updated for task {}", transactionId);
        } catch (Exception e) {
            logger.error("Can't update complited for task " + transactionId, e);
        }
    }

    public static void addStl1S3Path(long patientNumber, String stl1S3Path){
        String sql = "UPDATE analysis SET stl1_s3_path = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, stl1S3Path);
            preparedStatement.setLong(2, patientNumber);
            preparedStatement.executeUpdate();
            logger.info("stl1S3Path updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update stl1S3Path for patient " + patientNumber, e);
        }
    }
    public static void addCephS3Path(long patientNumber, String cephS3Path){
        String sql = "UPDATE analysis SET ceph_s3_path = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, cephS3Path);
            preparedStatement.setLong(2, patientNumber);
            preparedStatement.executeUpdate();
            logger.info("cephS3Path updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update cephS3Path for patient " + patientNumber, e);
        }

    }

    public static String getStlPath(long patientNumber) {
        String stl1_s3_path = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT stl1_s3_path FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                stl1_s3_path = rs.getString("stl1_s3_path");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
//            logger.info("Can't check is_dicom", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return stl1_s3_path;
    }
    public static String getCephPath(long patientNumber) {
        String ceph_s3_path = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT ceph_s3_path FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                ceph_s3_path = rs.getString("ceph_s3_path");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't get ceph path", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return ceph_s3_path;
    }

    public static boolean isDicom(long patientNumber) {
        boolean is_dicom = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT is_dicom FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                is_dicom = rs.getBoolean("is_dicom");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't check is_dicom", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return is_dicom;
    }

    public static void updateDicomProcess(long patientNumber, boolean isDicomProcess){

        String sql = "UPDATE analysis SET is_dicom_process = "+ (isDicomProcess? 1 : 0) +" WHERE Serial_Number = "+patientNumber;
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.executeUpdate(sql);
            logger.info("is_dicom_process updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update is_dicom_process for patient " + patientNumber, e);
        }
    }

    public static boolean isDicomCompleted(long patientNumber) {
        boolean is_dicom_completed = false;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT completed FROM dicom_tasks WHERE patient_id =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                is_dicom_completed = rs.getBoolean("completed");
            } else {
                is_dicom_completed = false;
            }
        } catch (Exception e) {
//            logger.info("Can't check is_dicom_completed", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return is_dicom_completed;
    }

    public static long isTaskExist(long patientNumber) {
        long is_task_exist = -1;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT transaction_id FROM dicom_tasks WHERE patient_id =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                is_task_exist = rs.getLong("transaction_id");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
//            logger.info("Can't check is_task_exist", e);
            return is_task_exist;
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return is_task_exist;
    }

    public static String getStl1Path(long patientNumber) {
        String stl1_s3_path = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT stl1_s3_path FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                stl1_s3_path = rs.getString("stl1_s3_path");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't select stl1_s3_path", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return stl1_s3_path;
    }

    public static List<String> getAllStl1Paths(final long patientNumber) {
        final List<String> paths = new ArrayList<>();
        final String sql = "SELECT stl1_s3_path FROM analysis WHERE (stl1_s3_path IS NOT NULL AND " +
            "stl1_s3_path<>\"\") AND (Serial_Number = ? OR (main_analysis_id=? and main_analysis_id<>0))";

        try (final PreparedStatement statement = connection.createPreparedStatement(sql))
        {
            statement.setLong(1, patientNumber);
            statement.setLong(2, patientNumber);
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                paths.add(rs.getString("stl1_s3_path"));
            }
        } catch (Exception e) {
            logger.info("Can't select stl1_s3_path", e);
        }
        return paths;
    }

    public static double getScale(long patientNumber) {
        double scale = 0;
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT scale FROM analysis WHERE Serial_Number =" + patientNumber;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                scale = rs.getDouble("scale");
            } else {
                throw new NoSuchPatientException(patientNumber);
            }
        } catch (Exception e) {
            logger.info("Can't select scale", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return scale;
    }

    public static boolean isCephExist(final long patientId) {
        boolean cephExist = false;
        Statement statement = connection.getStatement();
        String sql = "SELECT id FROM patpicks WHERE Pat_ID=" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            cephExist = rs.next();
        } catch (Exception e) {
            logger.info("Can't get id from patpicks table", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        if (!cephExist) {
            try {
                statement = connection.getStatement();
                sql="SELECT id FROM patient_images WHERE patient_ID=" + patientId+ " AND type='CEPH_MAIN'";
                rs = connection.execQuery(sql);
                cephExist = rs.next();
            } catch (Exception e) {
                logger.info("Can't get id from patient_images table", e);
            } finally {
                DBconnection.closeResources(rs, statement);
            }
        }
        if (!cephExist) {
            try {
                statement = connection.getStatement();
                sql="SELECT ceph_s3_path FROM analysis WHERE Serial_Number=" + patientId;
                rs = connection.execQuery(sql);
                cephExist = rs.next();
                if (cephExist) {
                    cephExist = !StringUtils.isBlank(rs.getString(1));
                }
            } catch (Exception e) {
                logger.info("Can't get id from patient_images table", e);
            } finally {
                DBconnection.closeResources(rs, statement);
            }
        }
        return cephExist;
    }

    public static boolean isZipStlExist(final long patientId) {
        boolean zipExists = false;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT zip_stl FROM analysis WHERE Serial_Number=" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            zipExists = rs.next() ? rs.getBoolean(1) : false;
        } catch (Exception e) {
            logger.info("Can't get zip_stl from analysis table", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return zipExists;
    }

    public static boolean isZipVideoExist(final long patientId) {
        boolean zipExists = false;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT zip_video FROM analysis WHERE Serial_Number=" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            zipExists = rs.next() ? rs.getBoolean(1) : false;
        } catch (Exception e) {
            logger.info("Can't get zip_video from analysis table", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return zipExists;
    }

    public static String stlJsonPath(final long patientId) {
        String jsonPath = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT json_path FROM analysis WHERE Serial_Number =" + patientId;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                jsonPath = rs.getString("json_path");
            } else {
                throw new NoSuchPatientException(patientId);
            }
        } catch (Exception e) {
            logger.info("Can't select json_path", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return jsonPath;
    }

    public static String shareLink(final String stlJsonPath) {
        return !StringUtils.isBlank(stlJsonPath) ? funcclass.resourcesUrl + "assets/plugins/stl_viewer_upd/STLViewer.html?json_location=" + stlJsonPath : "false";
    }

    public static boolean hasPanoramaImage(final long patientId) {
        boolean hasPanorama = false;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT 1 FROM patient_images WHERE (name='panaroma_mandibular.png' OR name='panaroma_maxillary.png') AND patient_id=" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            hasPanorama = rs.next();
        } catch (Exception e) {
            logger.info("Can't check has_panorama status ", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return hasPanorama;
    }

    public static Boolean hasAirwaysData(long patientId) {
        Boolean hasData = null;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT has_airways FROM analysis WHERE has_airways IS NOT NULL AND Serial_Number =" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            hasData = rs.next() ? rs.getBoolean(1) : null;

        } catch (Exception e) {
            logger.info("Can't check has_airways ", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return hasData;
    }

    public static Boolean checkAirwaysData(final long patientId) {
        Boolean hasAirwaysData = hasAirwaysData(patientId);
        if (hasAirwaysData == null) {
            hasAirwaysData = funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), getStl1Path(patientId) + "/" + AirwaysReportDataManager.VOLUME_JSON_FILE_NAME);
            updateHasAirways(patientId, hasAirwaysData);
        }
        return hasAirwaysData;
    }

    public static void updateHasAirways(final long patientId, final Boolean hasAirwaysData) {
        String sql = "UPDATE analysis SET has_airways = ? WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setBoolean(1, hasAirwaysData);
            preparedStatement.setLong(2, patientId);
            preparedStatement.executeUpdate();
            logger.info("has_airways updated for patient {}", patientId);
        } catch (Exception e) {
            logger.error("Can't update has_airways for patient " + patientId, e);
        }
    }

    public static Boolean dicomCaseIsTrial(long patientId) throws Exception {
        Boolean isTrial = false;
        if (getStlPath(funcclass.DEMO_PAT_ID).equals(getStlPath(patientId))) {
            return false;
        }
        final long doctorId = connection.getDocId(patientId);
        if (connection.getDocInfo(doctorId).hasBillingPlan) {
            final List<String> billingPlanFeatureNames = connection.getFeaturesNames(doctorId);
            isTrial = !billingPlanFeatureNames.contains("3D");
        }
        return isTrial;
    }

    public static void deletePatient(final Integer patientId) {
        String sql = "DELETE FROM analysis WHERE Serial_Number = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setInt(1, patientId);
            preparedStatement.executeUpdate();
            logger.info("Patient {} removed", patientId);
        } catch (Exception e) {
            logger.error("Can't remove patient " + patientId, e);
        }
    }

    public static void setAnalysisIsDemo(long patientNumber, boolean isDemo){
        final String sql = "UPDATE analysis SET is_demo = "+ (isDemo? 1 : 0) +" WHERE Serial_Number = "+patientNumber;
        try (Statement statement = connection.getConnection().createStatement()) {
            statement.executeUpdate(sql);
            logger.info("is_demo updated for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't update is_demo for patient " + patientNumber, e);
        }
    }

    public static Boolean case3dAllowed(final long patientId) throws Exception {
        final long doctorId =  connection.getDocIdByPatient(patientId);
        if (isDemo(patientId) || !connection.getDocInfo(doctorId).hasBillingPlan) {
            return true;
        }
        return connection.getFeaturesNames(doctorId).contains("3D cases");
    }

    public static Boolean isDemo(final long patientId) {
        Boolean demo = null;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT is_demo FROM analysis WHERE is_demo IS NOT NULL AND Serial_Number =" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            demo = rs.next() ? rs.getBoolean(1) : null;

        } catch (Exception e) {
            logger.info("Can't check has_airways ", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return demo;
    }


    public static boolean isCephFromCbctAllowed(final long patientId, final long doctorId) throws Exception {
        if (!connection.getDocInfo(doctorId).hasBillingPlan || isDemo(patientId)) {
            return true;
        }
        return connection.getFeaturesNames(doctorId).contains("Ceph from CBCT");
    }

    public static boolean isAirwaysVolumeAllowed(final long patientId, final long doctorId) throws Exception {
        if (!connection.getDocInfo(doctorId).hasBillingPlan || isDemo(patientId)) {
            return true;
        }
        return connection.getFeaturesNames(doctorId).contains("Airways volume");
    }

    public static boolean isDownloadVideoAllowed(final long patientId, final long doctorId) throws Exception {
        if (!connection.getDocInfo(doctorId).hasBillingPlan || isDemo(patientId)) {
            return true;
        }
        return connection.getFeaturesNames(doctorId).contains("Download video");
    }

    public static boolean isDownloadStlAllowed(final long patientId, final long doctorId) throws Exception {
        if (!connection.getDocInfo(doctorId).hasBillingPlan || isDemo(patientId)) {
            return true;
        }
        return connection.getFeaturesNames(doctorId).contains("Download STL");
    }

    public static boolean isViewStlAllowed(final long patientId, final long doctorId) throws Exception {
        if (!connection.getDocInfo(doctorId).hasBillingPlan || isDemo(patientId)) {
            return true;
        }
        return true;
    }

    public static boolean isFirstUploadedDicom(long patientId, final String doctorId) {
        boolean isSingleUpload = false;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT Serial_Number FROM analysis WHERE doctor_number=" + doctorId+"AND stl1_s3_path<>'' AND is_demo=false ORDER BY Serial_Number ASC";
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                isSingleUpload = rs.next() ? rs.getLong(1) == patientId : false;
            }

        } catch (Exception e) {
            logger.info("Can't check is_first_dicom", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return isSingleUpload;
    }

    public static String subAnalysesIds(final long id) {
        String subAnalysisIds = "";
        final Statement statement = connection.getStatement();
        final String sql = "SELECT Serial_Number FROM analysis WHERE main_analysis_id=" + id+" AND Serial_Number<>"+id;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            while (rs.next()) {
                subAnalysisIds+= rs.getLong(1)+",";
            }
        } catch (Exception e) {
            logger.info("Can't check is_first_dicom", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return subAnalysisIds;
    }

    public static String createStlReadOnlyGuid(final long patientId) {
        logger.info("Updating stl_read_only_guid for patient {} .", patientId);
        try {
            final String guid = UUID.randomUUID().toString();
            String sql = "UPDATE analysis SET stl_read_only_guid ='" + guid +
                "' WHERE Serial_Number = " + patientId;
            connection.execStatement(sql);
            return guid;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
            return "";
        }
    }

    public static String createStlReadWriteGuid(final long patientId) {
        logger.info("Updating stl_read_write_guid for patient {} .", patientId);

        try {
            final String guid = UUID.randomUUID().toString();
            String sql = "UPDATE analysis SET stl_read_write_guid ='" + guid +
                "' WHERE Serial_Number = " + patientId;
            connection.execStatement(sql);
            return guid;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
            return "";
        }
    }

    public static String stlReadOnlyGuid(final long patientId) {
        String guid = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT stl_read_only_guid FROM analysis WHERE Serial_Number =" + patientId;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                guid = rs.getString("stl_read_only_guid");
            } else {
                throw new NoSuchPatientException(patientId);
            }
        } catch (Exception e) {
            logger.info("Can't select stl_read_only_guid", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return guid;
    }

    public static String stlReadWriteGuid(final long patientId) {
        String guid = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        String sql = "SELECT stl_read_write_guid FROM analysis WHERE Serial_Number =" + patientId;
        try {
            rs = connection.execQuery(sql);
            if (rs.next()) {
                guid = rs.getString("stl_read_write_guid");
            } else {
                throw new NoSuchPatientException(patientId);
            }
        } catch (Exception e) {
            logger.info("Can't select stl_read_write_guid", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return guid;
    }

    public static void createStlReadOnlyLink(final long patientId, final String link) {
        logger.info("Updating stl_read_only_link for patient {} .", patientId);
        try {
            String sql = "UPDATE analysis SET stl_read_only_link ='" + link +
                "' WHERE Serial_Number = " + patientId;
            connection.execStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
    }

    public static void createStlReadWriteLink(final long patientId, final String link) {
        logger.info("Updating stl_read_write_link for patient {} .", patientId);
        try {
            String sql = "UPDATE analysis SET stl_read_write_link ='" + link +
                "' WHERE Serial_Number = " + patientId;
            connection.execStatement(sql);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
    }

    public static String stlReadOnlyLink(final long patientId) {
        String link = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            final String sql = "SELECT stl_read_only_link FROM analysis " +
                " WHERE Serial_Number = " + patientId;
            rs = connection.execQuery(sql);
            if (rs.next()) {
                link = rs.getString("stl_read_only_link");
            } else {
                throw new NoSuchPatientException(patientId);
            }
        } catch (Exception e) {
            logger.info("Can't select stl_read_write_link", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return link;
    }

    public static String stlReadWriteLink(final long patientId) {
        String link = "";
        Statement statement = connection.getStatement();
        ResultSet rs = null;
        try {
            final String sql = "SELECT stl_read_write_link FROM analysis " +
                " WHERE Serial_Number = " + patientId;
            rs = connection.execQuery(sql);
            if (rs.next()) {
                link = rs.getString("stl_read_write_link");
            } else {
                throw new NoSuchPatientException(patientId);
            }
        } catch (Exception e) {
            logger.info("Can't select stl_read_write_link", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return link;
    }

    public static List<String> getFailedRequestsJsons() {
        final List<String> jsons = new ArrayList<>();
        final String sql = "SELECT request_json FROM dicom_requests WHERE is_skipped=0 AND is_proceeded=0 ORDER BY datetime";

        try (final PreparedStatement statement = connection.createPreparedStatement(sql)) {
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                jsons.add(rs.getString("request_json"));
            }
        } catch (Exception e) {
            logger.info("Can't select request_json", e);
        }
        return jsons;
    }

    public static String imageS3Key (final long patientId, final String imageName) {
        String s3Key = "";
        final String sql = "SELECT s3_path FROM patient_images WHERE patient_id=? and name=?";
        try (final PreparedStatement statement = connection.createPreparedStatement(sql)) {
            statement.setLong(1, patientId);
            statement.setString(2, imageName);
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                s3Key = rs.getString("s3_path");
            }
        } catch (Exception e) {
            logger.info("Can't select s3_path", e);
        }
        return s3Key;
    }

    public static void processLeftProjection(final long patientNum, final long doctorNum, final String s3Key) {
        try {
            long startAlgoCephTime = System.currentTimeMillis();
            system.log("doctor is set for auto traced!!!");

            DBconnection db = DBconnection.GetDBconnection();

            AlgoCephClient algoCephClient = AlgoCephClient.instance();
            Map traceResult = algoCephClient.autoTraceCephForLeftProjection(patientNum, doctorNum, s3Key);

            AutoTraceData jsonData = null;
            if (traceResult != null) {
                jsonData = traceResult.get("data") == null ? null : (AutoTraceData) traceResult.get("data");
                if (jsonData != null) {
                    logger.info("ScaleConfidence: " + jsonData.getScaleData().getConfidence());
                } else {
                    logger.info("jsonData = null");
                }
            }

            long timeAfterResponseFromalgoCephServer = System.currentTimeMillis();

            system.log("timeAfterResponseFromalgoCephServer is " + (timeAfterResponseFromalgoCephServer - startAlgoCephTime) + " milliseconds");

            PatientAnalysis pa = jsonData.toPatientAnalysis(patientNum);
            addLeftProjectionAnalysis(jsonData,patientNum,doctorNum);

            int imgHeightFromjson = Integer.parseInt(jsonData.getImgSize().getHeight());
            int imgWidthFromJson = Integer.parseInt(jsonData.getImgSize().getWidth());

            boolean scaleFail = true;
            pa.setScaleFromAlgoCephScaleCoords(jsonData.getScaleData().getPoints(), imgHeightFromjson, imgWidthFromJson);
//            if (jsonData.getScaleData().getConfidence() >= CephxParameters.getAlgoScaleConfidenceLvlParam().getValueAsDouble()) {
//                pa.setScaleFromAlgoCephScaleCoords(jsonData.getScaleData().getPoints(), imgHeightFromjson, imgWidthFromJson);
//                scaleFail = false;
//            }
//
//            if (scaleFail) {
//                if (DoctorService.isDoctorAllowedForScaleEstimation(doctorNum)) {
//                    pa.setScaleFromAlgoCephTeethLandmarks(jsonData.getCoordinates(), Integer.parseInt(jsonData.getImgSize().getHeight()), Integer.parseInt(jsonData.getImgSize().getWidth()));
//                }
//            }
            pa.convertPointsFromAlgoCeph(Integer.parseInt(jsonData.getImgSize().getHeight()), Integer.parseInt(jsonData.getImgSize().getWidth()));

            LandmarkService.updateLeftProjectionLandmarks(pa);
            try {
                LandmarkService.saveLeftProjectionOriginalLandmarksAfterAlgo(pa.NewPat.GetNumber(), pa.NewPat.patPoints.GenPoints.getPointList());
            } catch (Exception e) {
                logger.warn("Original landmarks after algo already exist", e);
            }
            connection.setLeftProjectionScale(patientNum, pa.ScaleFactor);
            connection.setLeftProjectionAlgoScaleCoords(jsonData.getScaleData().getPoints());
        } catch (Exception e) {
            throw e;
        }
    }

    private static void addLeftProjectionAnalysis(AutoTraceData jsonData, long patientId, final long doctorId) {
        final String algoS3Key = jsonData.getImgUrl().getKey();
        final String commonS3Key = funcclass.getS3Key(patientId, doctorId, "Ceph_left");
        funcclass.s3client.copyObject(funcclass.getS3BucketName(),algoS3Key, funcclass.getS3BucketName(),commonS3Key);
        DBconnection.GetDBconnection().addLeftProjectionAnalysis(patientId,commonS3Key);;
    }

    public static boolean hasLeftProjection(final long patientId) {
        boolean hasProjection = hasLeftProjectionLandmarks(patientId);
        if (!hasProjection) {
            final Statement statement = connection.getStatement();
            final String sql = "SELECT 1 FROM patient_images WHERE patient_id=" + patientId + " AND name='" + funcclass.LEFT_PROJECTION_FILE_NAME + "'";
            ResultSet rs = null;
            try {
                rs = connection.execQuery(sql);
                hasProjection = rs.next();
            } catch (Exception e) {
                logger.info("Can't check left projection image", e);
            } finally {
                DBconnection.closeResources(rs, statement);
            }
        }
        return hasProjection;
    }

    public static boolean hasLeftProjectionLandmarks(final long patientId) {
        boolean hasLandmarks = false;
        final Statement statement = connection.getStatement();
        final String sql = "SELECT 1 FROM analysiscoordinates_left WHERE patient_number=" + patientId;
        ResultSet rs = null;
        try {
            rs = connection.execQuery(sql);
            hasLandmarks = rs.next();
        } catch (Exception e) {
            logger.info("Can't check left projection landmarks", e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return hasLandmarks;
    }

    public static boolean hasAsymmetryAnalysis(final Long id) {
        return isCephExist(id) && hasLeftProjection(id);

    }
}
