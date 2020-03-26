package com.cephx.def.service.db;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.cephx.def.DBconnection;
import com.cephx.def.funcclass;
import com.cephx.def.struct.struct.PictureEntry;
import com.cephx.def.util.date.DateUtility;
import com.cephx.def.util.string.RandomString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;


public class PictureService {
    private static DBconnection connection = DBconnection.GetDBconnection();
    private final static Logger logger = LogManager.getLogger(PatientService.class);

    public static void deletePicture(long pictureNumber) {
        try {
            connection.execStatement("DELETE FROM patpicks WHERE ID=" + pictureNumber);
            logger.info("Picture {} was deleted succesfully", pictureNumber);
        } catch (Exception e) {
            logger.error("Can't delete picture " + pictureNumber, e);
        }
    }

    public static List<PictureEntry> getNotConvertedPicturesForPatient(long patientNumber) {
        String sql = "SELECT * FROM patpicks WHERE Pat_ID = ? AND converted = ?";
        ResultSet rs = null;
        List<PictureEntry> pictures = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, patientNumber);
            preparedStatement.setBoolean(2, false);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PictureEntry pe = new PictureEntry();
                pe.setId(rs.getLong("ID"));
                pe.setDocID(rs.getLong("Doc_ID"));
                pe.setPatID(rs.getLong("Pat_ID"));
                pe.setOrder(rs.getInt("PicOrder"));
                pe.setImgName(rs.getString("Name"));
                pe.setType(rs.getString("PicType"));
                pe.setConverted(rs.getBoolean("converted"));
                pe.setExtension(rs.getString("extension"));
                pictures.add(pe);
            }
        } catch (Exception e) {
            logger.info("Can't get not converted pictures for patient " + patientNumber, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                logger.error("Can't close resultSet", e);
            }

        }
        return pictures;
    }

    public static void insertPicture(PictureEntry picture) {
        String sql = "INSERT INTO patpicks (Pat_ID,Doc_ID,Name,PicOrder,PicType) values(?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, picture.getPatID());
            preparedStatement.setLong(2, picture.getDocID());
            preparedStatement.setString(3, picture.getImgName());
            preparedStatement.setInt(4, picture.getOrder());
            preparedStatement.setString(5, picture.getType());
            preparedStatement.executeUpdate();
            logger.info("Picture was inserted: {}", picture);
        } catch (Exception e) {
            logger.error("Can't insert picture", e);
        }
    }

    public static PictureEntry getPictureById(long pictureNumber) {
        PictureEntry picture = null;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT * FROM patpicks WHERE ID=" + pictureNumber, statement);
            if (rs.next()) {
                picture = new PictureEntry();
                picture.setId(rs.getLong("ID"));
                picture.setDocID(rs.getLong("Doc_ID"));
                picture.setPatID(rs.getLong("Pat_ID"));
                picture.setOrder(rs.getInt("PicOrder"));
                picture.setImgName(rs.getString("Name"));
                picture.setType(rs.getString("PicType"));
                picture.setConverted(rs.getBoolean("converted"));
                picture.setNewImageId(rs.getLong("new_image_id"));
                picture.setExtension(rs.getString("extension"));
            }
        } catch (Exception e) {
            logger.error("Can't get picture by id " + pictureNumber, e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return picture;
    }

    public static PictureEntry getPictureByNewId(long newImageId) {
        PictureEntry picture = null;
        ResultSet rs = null;
        Statement statement = connection.getStatement();
        try {
            rs = connection.execQuery("SELECT * FROM patpicks WHERE new_image_id=" + newImageId, statement);
            if (rs.next()) {
                picture = new PictureEntry();
                picture.setId(rs.getLong("ID"));
                picture.setDocID(rs.getLong("Doc_ID"));
                picture.setPatID(rs.getLong("Pat_ID"));
                picture.setOrder(rs.getInt("PicOrder"));
                picture.setImgName(rs.getString("Name"));
                picture.setType(rs.getString("PicType"));
                picture.setConverted(rs.getBoolean("converted"));
                picture.setNewImageId(rs.getLong("new_image_id"));
                picture.setExtension(rs.getString("extension"));
            }
        } catch (Exception e) {
            logger.error("Can't get picture by new_image_id " + newImageId, e);
        } finally {
            DBconnection.closeResources(rs, statement);
        }
        return picture;
    }

    public static void updateConverted(boolean converted, long newImageId) {
        String sql = "UPDATE patpicks SET converted = ?, new_image_id = ? WHERE new_image_id = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setBoolean(1, converted);
            preparedStatement.setLong(2, 0);
            preparedStatement.setLong(3, newImageId);
            preparedStatement.executeUpdate();
            logger.info("Successfully changed converted  to {} of picture", converted);
        } catch (Exception e) {
            logger.info("Can't update converted option of picture ", e);
        }
    }

    public static void updateConverted(long pictureId, boolean converted, long newImageId) {
        String sql = "UPDATE patpicks SET converted = ?, new_image_id = ? WHERE ID = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setBoolean(1, converted);
            preparedStatement.setLong(2, newImageId);
            preparedStatement.setLong(3, pictureId);
            preparedStatement.executeUpdate();
            logger.info("Successfully changed converted  to {} of picture {}", converted, pictureId);
        } catch (Exception e) {
            logger.info("Can't update converted option of picture " + pictureId, e);
        }
    }

    public static void updateS3Expiration(long pictureId, String s3Key, Date expirationDate) {
        String sql = "UPDATE patpicks SET s3_key = ?, expire_key = ? WHERE ID = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, s3Key);
            preparedStatement.setDate(2, DateUtility.toSqlDate(expirationDate));
            preparedStatement.setLong(3, pictureId);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            logger.info("Can't update" + pictureId, e);
        }
    }

    public static PictureEntry[] getPatientPictures(long patientNumber, long doctorNumber) {
        String sql = "SELECT * FROM patpicks WHERE Pat_ID = ? AND Doc_Id = ? ORDER BY picOrder";
        ResultSet rs = null;
        Vector v = new Vector();
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setLong(1, patientNumber);
            preparedStatement.setLong(2, doctorNumber);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                PictureEntry pe = new PictureEntry();
                pe.setId(rs.getLong("ID"));
                pe.setDocID(rs.getLong("Doc_ID"));
                pe.setPatID(rs.getLong("Pat_ID"));
                pe.setOrder(rs.getInt("PicOrder"));
                pe.setImgName(rs.getString("Name"));
                pe.setType(rs.getString("PicType"));
                pe.setExtension(rs.getString("extension"));
                v.addElement(pe);
            }
        } catch (Exception e) {
            logger.info("Can't update algo_ceph_quality of patient " + patientNumber, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                logger.error("Can't close resultSet", e);
            }

        }
        PictureEntry[] pictures = new PictureEntry[v.size()];
        for (int i = 0; i < pictures.length; ++i) {
            pictures[i] = (PictureEntry) v.elementAt(i);
        }
        return pictures;
    }


//    public static List<PictureEntry> getPatientPictures(long patientNumber, long doctorNumber) {
//        String sql = "SELECT * FROM patpicks WHERE Pat_ID = ? AND Doc_Id = ? ORDER BY picOrder";
//        ResultSet rs = null;
//        List<PictureEntry> pictures = new ArrayList<>();
//        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
//            preparedStatement.setLong(1, patientNumber);
//            preparedStatement.setLong(2, doctorNumber);
//            rs = preparedStatement.executeQuery();
//            while (rs.next()) {
//                PictureEntry pe = new PictureEntry();
//                pe.setId(rs.getLong("ID"));
//                pe.setDocID(rs.getLong("Doc_ID"));
//                pe.setPatID(rs.getLong("Pat_ID"));
//                pe.setOrder(rs.getInt("PicOrder"));
//                pe.setImgName(rs.getString("Name"));
//                pe.setType(rs.getString("PicType"));
//                pictures.add(pe);
//            }
//        } catch (Exception e) {
//            logger.info("Can't get patient picture ", e);
//        } finally {
//            try {
//                if (rs != null) {
//                    rs.close();
//                }
//            } catch (Exception e) {
//                logger.error("Can't close resultSet", e);
//            }
//
//        }
//        return pictures;
//    }

    public static void updatePictureType(long pictureId, String type) {
        String sql = "UPDATE patpicks SET PicType = ? WHERE ID = ?";
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, type);
            preparedStatement.setLong(2, pictureId);
            preparedStatement.executeUpdate();
            logger.info("Successfully changed type  to {} of picture {}", type, pictureId);
        } catch (Exception e) {
            logger.info("Can't update type of picture " + pictureId, e);
        }
    }

    public static String getImagePathFromS3Bucket(PictureEntry image) throws Exception {
        String imageType = image.getType();
        String imageName = image.getImgName();
        long patientNumber = image.getPatID();
        long doctorNumber = image.getDocID();
        int imageOrder = image.getOrder();


        String bucket = "temp." + funcclass.getS3BucketName();
        String key;
        String keyWithExpire;
        PictureEntry picture = DBconnection.GetDBconnection().getPictureEntry(patientNumber, imageName);
        if(picture == null)
            return null;

        keyWithExpire = picture.getS3Key();
        PictureEntry[] cephs = DBconnection.GetDBconnection().getPatientCephPictureEntries(patientNumber, doctorNumber);
        switch (imageType) {
            case funcclass.PIC_TYPE_CEPH:
                key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH);
                if (cephs.length > 1 && imageOrder > 1) {
                    key = funcclass.getS3OutDatedKey(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH, imageOrder);
                }
                break;

            case funcclass.PIC_TYPE_PROFILE:
                String [] profiles = DBconnection.GetDBconnection().getPatProfilesNames(patientNumber +"");
                if(profiles == null || profiles.length ==0)
                    return null;
                key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE);
                break;

            case funcclass.PIC_TYPE_PANORAMIC:
                key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PANORAMIC);
                break;

            case funcclass.PIC_TYPE_INTRAORAL:
                key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_INTRAORAL);
                break;

            case funcclass.PIC_TYPE_OTHER:
                key = funcclass.getS3Key(patientNumber, doctorNumber, imageName);
                break;

            default:
                key = funcclass.getS3Key(patientNumber, doctorNumber, imageType);
                break;
        }


        Date date = new Date();
        if (keyWithExpire == null || !funcclass.s3clientNotEncrypted.doesObjectExist(bucket, keyWithExpire) || checkifLinkExpired(picture.getExpire())) {

            String easy = RandomString.digits + "ACEFGHJKLMNPQRUVWXYabcdefhijkprstuvwx";
            RandomString randomString = new RandomString(64, new SecureRandom(), easy);
            keyWithExpire = randomString.nextString();


            File file;

            if (imageType.equals(funcclass.PIC_TYPE_OTHER)) {
                file = funcclass.getFileFromS3ByKey(patientNumber, doctorNumber, key, imageName);
            } else {
                if (cephs.length > 1 && imageType.equals(funcclass.PIC_TYPE_CEPH) && imageOrder > 1) {
                    file = funcclass.getOutdatedCephFileFromS3(patientNumber, doctorNumber, imageOrder);
                } else {
                    file = funcclass.getFileFromS3ByKey(patientNumber, doctorNumber, key, imageType);
                }

            }

            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.DATE, 1);
            date = c.getTime();


            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType("image/png");
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket,
                    keyWithExpire, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead)
                    .withMetadata(objectMetadata);

            funcclass.s3clientNotEncrypted.putObject(putObjectRequest);
            DBconnection.GetDBconnection().updatePictureS3Expiration(picture.getId(), keyWithExpire, date);

            if (file != null && file.exists())
                file.delete();
        } else {
            File file;

            if (imageType.equals(funcclass.PIC_TYPE_OTHER)) {
                file = new File(funcclass.getPathForPatientOther(patientNumber, doctorNumber, imageName.replace(funcclass.PIC_TYPE_OTHER + "_", "")));
            } else {
                file = new File(funcclass.getDirPathForPatient(patientNumber, doctorNumber) + "~~Ceph~~.jpg");
            }

            if (file != null && file.exists())
                file.delete();
        }
        return funcclass.s3UrlTemp + keyWithExpire;
    }

    private static boolean checkifLinkExpired(Date expire) {
        if(expire == null)
            return true;
        Date date = new Date();

        return date.after(expire);
    }

    public static PictureEntry getPatientPictureByName(final String key, final long patientId) {
        String sql = "SELECT * FROM patpicks WHERE name=? AND Pat_ID=?";
        ResultSet rs = null;
        PictureEntry pe = null;
        try (PreparedStatement preparedStatement = connection.createPreparedStatement(sql)) {
            preparedStatement.setString(1, key);
            preparedStatement.setLong(2, patientId);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                pe = new PictureEntry();
                pe.setId(rs.getLong("ID"));
                pe.setDocID(rs.getLong("Doc_ID"));
                pe.setPatID(rs.getLong("Pat_ID"));
                pe.setOrder(rs.getInt("PicOrder"));
                pe.setImgName(rs.getString("Name"));
                pe.setType(rs.getString("PicType"));
                pe.setExtension(rs.getString("extension"));
            }
        } catch (Exception e) {
            logger.info("Can't get patient picture by s3 key " + key, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                logger.error("Can't close resultSet", e);
            }

        }
        return pe;
    }

}
