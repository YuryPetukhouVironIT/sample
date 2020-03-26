package com.cephx.def.service.db;

import com.cephx.def.*;
import com.cephx.def.util.string.StringUtility;
import com.google.gson.internal.LinkedHashTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Vector;

public class LandmarkService {
    private static DBconnection connection = DBconnection.GetDBconnection();
    private final static Logger logger = LogManager.getLogger(LandmarkService.class);

    /**
     * Inserts original landmarks after algo
     * @param patientNumber Number of patient
     * @param landmarks Original landmarks
     */
    public static void saveOriginalLandmarksAfterAlgo(long patientNumber, List<LinkedHashTreeMap> landmarks) {
        double x;
        double y;
        Double pointNumber;
        Double groupNumber;
        Double numberOfPointInGroup;
        BigDecimal confidence = null;
        String sql = "INSERT INTO original_landmarks" +
                "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, owner) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            for (int i = 1; i <= landmarks.size(); i++) {
                LinkedHashTreeMap map = landmarks.get(i - 1);
                pointNumber = Double.parseDouble(map.get("Number").toString());
                if (pointNumber != -1) {
                    groupNumber = Double.parseDouble(map.get("GroupNum").toString());
                    numberOfPointInGroup = Double.parseDouble(map.get("PointInGroup").toString());
                    x = StringUtility.stringIsDouble(map.get("x_cord").toString()) ? Double.parseDouble(map.get("x_cord").toString()) : 0;
                    y = StringUtility.stringIsDouble(map.get("y_cord").toString()) ? Double.parseDouble(map.get("y_cord").toString()) : 0;
                    confidence = StringUtility.stringIsBigDecimal(map.get("confidence").toString()) ? new BigDecimal(map.get("confidence").toString()) : new BigDecimal(1);
                    preparedStatement.setLong(1, patientNumber);
                    preparedStatement.setDouble(2, x);
                    preparedStatement.setDouble(3, y);
                    preparedStatement.setInt(4, pointNumber.intValue());
                    preparedStatement.setInt(5, numberOfPointInGroup.intValue());
                    preparedStatement.setInt(6, groupNumber.intValue());
                    preparedStatement.setBigDecimal(7, confidence);
                    preparedStatement.setString(8, "Algo");
                    preparedStatement.execute();

                }
            }
            logger.info("Original landmarks after algo are saved successfully for patient {}", patientNumber);
        } catch (Exception e) {
            if ((e.getMessage()!=null) && (!e.getMessage().contains("Duplicate entry '0-1-Algo'"))) {
                logger.error("Error during saving original landmarks", e);
            }

        }
    }

        public static void saveDefaultLandmarks(long patientNumber) {
        double x;
        double y;
        Double pointNumber;
        Double groupNumber;
        Double numberOfPointInGroup;
        BigDecimal confidence = null;
        String sql = "INSERT INTO original_landmarks" +
                "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, owner) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
//            for (int i = 1; i <= landmarks.size(); i++) {
//                LinkedHashTreeMap map = landmarks.get(i - 1);
//                pointNumber = Double.parseDouble(map.get("Number").toString());
//                if (pointNumber != -1) {
//                    groupNumber = Double.parseDouble(map.get("GroupNum").toString());
//                    numberOfPointInGroup = Double.parseDouble(map.get("PointInGroup").toString());
//                    x = Double.parseDouble(map.get("x_cord").toString());
//                    y = Double.parseDouble(map.get("y_cord").toString());
//                    if (map.get("confidence") != null) {
//                        confidence = new BigDecimal(map.get("confidence").toString());
//                    }
//
//                    preparedStatement.setLong(1, patientNumber);
//                    preparedStatement.setDouble(2, x);
//                    preparedStatement.setDouble(3, y);
//                    preparedStatement.setInt(4, pointNumber.intValue());
//                    preparedStatement.setInt(5, numberOfPointInGroup.intValue());
//                    preparedStatement.setInt(6, groupNumber.intValue());
//                    preparedStatement.setBigDecimal(7, confidence);
//                    preparedStatement.setString(8, "Algo");
//                    preparedStatement.execute();
//                }
//            }
            logger.info("Original landmarks after algo are saved successfully for patient {}", patientNumber);
        } catch (Exception e) {
                logger.error("Error during saving original landmarks", e);
        }
    }

    /**
     * Inserts original patients points after operator's approving
     * @param patientNumber Number of patient
     */
    public static void saveOriginalLandmarksAfterOperator(long patientNumber) {
        String sql = "INSERT INTO original_landmarks " +
                "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, owner) " +
                "SELECT patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, 'Operator' " +
                "FROM analysiscoordinates WHERE patient_number=" + patientNumber;
        try {
            connection.execStatement(sql);
            logger.info("Original landmarks after operator are saved successfully for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't set original landmarks for patient " + patientNumber, e);
        }
    }

    public static void insertLandmarks(final long patientNumber, final Vector<cpoint> landmarks, final boolean isLeftProjection) throws Exception {
        Vector DocVec = new Vector();
        double x = 0;
        double y = 0;
        long pointNumber = 0;
        long groupNumber = 0;
        long numberOfPointInGroup = 0;
        BigDecimal confidence = null;
        String sql = "INSERT INTO analysiscoordinates" +
                "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        if (isLeftProjection) {
            sql = "INSERT INTO analysiscoordinates_left" +
                "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            for (int i = 1; i <= landmarks.size(); i++) {
                pointNumber = landmarks.elementAt(i - 1).get_number();
                if (pointNumber != -1) {
                    groupNumber = landmarks.elementAt(i - 1).GroupNum;
                    numberOfPointInGroup = landmarks.elementAt(i - 1).PointInGroup;
                    x = landmarks.elementAt(i - 1).x_cord;
                    y = landmarks.elementAt(i - 1).y_cord;
                    confidence = landmarks.elementAt(i - 1).confidence;

                    preparedStatement.setLong(1, patientNumber);
                    preparedStatement.setDouble(2, x);
                    preparedStatement.setDouble(3, y);
                    preparedStatement.setLong(4, pointNumber);
                    preparedStatement.setLong(5, numberOfPointInGroup);
                    preparedStatement.setLong(6, groupNumber);
                    preparedStatement.setBigDecimal(7, confidence);
                    preparedStatement.execute();
                } else {
                    DocVec.addElement(landmarks.elementAt(i - 1));
                }
            }
            logger.info("Landmarks are saved successfully for patient {}", patientNumber);
            if (DocVec.size() > 0) {
                connection.UpdateDocPoints(DocVec, patientNumber);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public static void updateLeftProjectionLandmarks(final PatientAnalysis pa) {
        try {
            Vector<cpoint> V = pointsVector(pa);
            if (!V.isEmpty()) {
                long patientNumber = pa.NewPat.GetNumber();
                if (!PatientService.isDigitizedPatient(patientNumber)) {
                    insertLandmarks(patientNumber, V, true);
                } else {
                    Vector<cpoint> pointsToAdd = connection.UpdatePointsAndGetPointsToAdd(V, patientNumber, true);
                    if (!pointsToAdd.isEmpty()) {
                        for (cpoint point : pointsToAdd) {
                            point.GroupNum = connection.getGroupNumberByTotalPointNumber(point.get_number());
                            point.PointInGroup = connection.getPointNumberInGroupByName(point.get_number());
                        }
                        insertLandmarks(patientNumber, pointsToAdd, true);
                    }

                }
                connection.setLeftProjectionScale(patientNumber, pa.ScaleFactor);
            }
        } catch (Exception ex) {
            logger.info("Error during updating landmarks for patient " + pa.NewPat.GetNumber(), ex);
        }
    }



    public static void saveLeftProjectionOriginalLandmarksAfterAlgo(long patientNumber, List pointList) {
        String sql = "INSERT INTO original_landmarks_left " +
            "(patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, owner) " +
            "SELECT patient_number,x_coordinate,y_coordinate,total_point_number,point_in_group,group_number, confidence, 'Operator' " +
            "FROM analysiscoordinates_left WHERE patient_number=" + patientNumber;
        try {
            connection.execStatement(sql);
            logger.info("Original landmarks after operator are saved successfully for patient {}", patientNumber);
        } catch (Exception e) {
            logger.error("Can't set original landmarks for patient " + patientNumber, e);
        }
    }

    public static void updateLandmarks(final PatientAnalysis pa, final long doctorNumber) {

        try {
            Vector<cpoint> V = pointsVector(pa);

            if (!V.isEmpty()) {
                long patientNumber = pa.NewPat.GetNumber();
                if (!PatientService.isDigitizedPatient(patientNumber)) {
                    insertLandmarks(patientNumber, V, false);
                    PatientService.setDigitized(patientNumber, true);
                    if (pa.NewPat.AdvancedInfo.algoCephQuality != null) {
                        PatientService.updateAlgoQualityForAnalysis(patientNumber, pa.NewPat.AdvancedInfo.algoCephQuality);
                    }
                    //check if owner is operator
                    if (pa.NewPat.AdvancedInfo.owner == funcclass.OPERATOR) {
                        PatientService.updateOperatorNumber(patientNumber, doctorNumber);
                        PatientService.updateTraceTime(patientNumber);
                    }
                } else {
                    Vector<cpoint> pointsToAdd = connection.UpdatePointsAndGetPointsToAdd(V, patientNumber, false);
                    if (!pointsToAdd.isEmpty()) {
                        for (cpoint point : pointsToAdd) {
                            point.GroupNum=connection.getGroupNumberByTotalPointNumber(point.get_number());
                            point.PointInGroup=connection.getPointNumberInGroupByName(point.get_number());
                        }
                        insertLandmarks(patientNumber,pointsToAdd, false);
                    };
                }
                connection.setScale(patientNumber, pa.ScaleFactor);
                if (pa.NewPat.AdvancedInfo.owner == funcclass.OPERATOR) {
                    //insert record into traces table
                    connection.patientTracedStamp(patientNumber, doctorNumber);
                }
            }

        } catch (Exception ex) {
            logger.info("Error during updating landmarks for patient " + pa.NewPat.GetNumber(), ex);
        }
    }

    private static Vector pointsVector(final PatientAnalysis pa) throws Exception {
        Vector<cpoint> V = new Vector<>();
        Vector points = pa.NewPat.patPoints.GenPoints.getVec();
        for (int i = 0; i < points.size(); i++) {

            if (points.elementAt(i) == null) {
                continue;
            }

            com.google.gson.internal.LinkedHashTreeMap map = (LinkedHashTreeMap) points.elementAt(i);

            if (map.size() == 0 || map == null) {
                continue;
            }

            final double x = StringUtility.stringIsDouble(map.get("x_cord")) ? Double.parseDouble(map.get("x_cord").toString()) : 0;
            final double y = StringUtility.stringIsDouble(map.get("y_cord")) ? Double.parseDouble(map.get("y_cord").toString()) : 0;
            final BigDecimal confidence = StringUtility.stringIsBigDecimal(map.get("confidence")) ? new BigDecimal(map.get("confidence").toString()) : new BigDecimal(1);
            final Double Number = Double.parseDouble(map.get("Number").toString());
            final String GroupName = map.get("GroupName").toString();
            final String Name = map.get("Name").toString();
            final Double GroupNum = Double.parseDouble(map.get("GroupNum").toString());
            final Double PointInGroup = Double.parseDouble(map.get("PointInGroup").toString());

            cpoint cp = new cpoint(x, y, confidence, Name, Number.intValue(), GroupName, GroupNum.intValue(), PointInGroup.intValue());
            V.addElement(cp);
        }
        return V;
    }
}
