package com.company.def.service.db;

import com.company.def.BasicDoc;
import com.company.def.Cdoctor;
import com.company.def.DBconnection;
import com.company.def.enums.PictureType;
import com.company.def.exceptions.DublicatePrimaryImageException;
import com.company.def.exceptions.ImageLimitException;
import com.company.def.funcclass;
import com.company.def.model.PatientImage;
import com.company.def.repository.PatientImageRepository;
import com.company.def.repository.TemplateImageRepository;
import com.company.def.service.S3Service;
import com.company.def.servlets.patient.ImageConverter;
import com.company.def.struct.struct.PictureEntry;
import com.company.def.util.date.DateUtility;
import com.company.def.util.file.FileUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Service
public class PatientImageService {
    private static final Logger logger = LogManager.getLogger(PatientImageService.class);
    private static final String DEFAULT_IMAGE_EXTENSION = "JPG";

    @Autowired
    private PatientImageRepository patientImageRepository;
    @Autowired
    private TemplateImageRepository templateImageRepository;
    @Autowired
    private S3Service s3Service;


    @Transactional
    public long savePatientImage(PatientImage patientImage) {
        return patientImageRepository.insert(patientImage);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long saveImageFromUploader(PatientImage patientImage, Cdoctor doctor) {
        long imageId = savePatientImage(patientImage);
        if (doctor.docInfo.getMaxUploadedImagesCount() < getPatientImagesCount(patientImage.getPatientNumber())) {
            throw new ImageLimitException();
        }
        return imageId;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long saveImageFromUploaderWithDocInfo(PatientImage patientImage, BasicDoc docInfo) {
        long imageId = savePatientImage(patientImage);
        if (docInfo.getMaxUploadedImagesCount() < getPatientImagesCount(patientImage.getPatientNumber())) {
            throw new ImageLimitException();
        }
        return imageId;
    }

    @Transactional
    public void deletePatientImage(long imageId) {
        s3Service.removePatientImage(s3Service.getS3KeyForPatientImage(imageId));
        s3Service.removePatientImage(s3Service.getS3KeyForPatientImageThumbnail(imageId));
        templateImageRepository.remove(imageId);
//        PictureService.updateConverted(false, imageId);
        patientImageRepository.remove(imageId);
    }

    public PatientImage getPatientImage(long imageId) {
        return patientImageRepository.findById(imageId);
    }

    public void changeImageType(long imageId, PictureType type) {
        patientImageRepository.updateImageType(imageId, type.name());
    }

    public void updateTypeAndPrimary(long imageId, PictureType type, boolean primary) {
        patientImageRepository.updateTypeAndPrimary(imageId, primary, type.name());
    }

    public void changePrimaryOfImage(long imageId, boolean primary) {
        patientImageRepository.updatePrimary(imageId, primary);
    }

    public void updateExpirationForImage(PatientImage patientImage) {
        patientImageRepository.updateExpirationForImage(patientImage.getId(), DateUtility.toSqlDate(patientImage.getExpireDate_image()),
                patientImage.getS3Key_image());
    }

    public void updateExpirationForThumbnail(PatientImage patientImage) {
        patientImageRepository.updateExpirationForThumbnail(patientImage.getId(), DateUtility.toSqlDate(patientImage.getExpireDate_thumbnail()),
                patientImage.getS3Key_thumbnail());
    }

    public List<PatientImage> getPatientImages(long patientNumber) {
        return patientImageRepository.getImagesForPatient(patientNumber);
    }

    public long getPrimaryImageIdForType(PictureType type, long patientNumber) {
        return patientImageRepository.getImageId(patientNumber, type.name(), true);
    }

    public boolean isLinkExpiredForImage(PatientImage image) {
        String bucket = "temp." + funcclass.getS3BucketName();
        String s3Key = image.getS3Key_image();
        boolean isLinkExpired = false;
        if(image.getExpireDate_image() == null) {
           isLinkExpired = true;
        } else {
            Date date = new Date();
            isLinkExpired = date.after(image.getExpireDate_image());
        }
        return s3Key == null
                || !funcclass.s3clientNotEncrypted.doesObjectExist(bucket, s3Key)
                || isLinkExpired;
    }

    public List<PatientImage> getPatientImagesForTypes(long patientNumber, Set<PictureType> types) {
        return patientImageRepository.find(patientNumber, PictureType.getPictureTypesAsList(types), true);
    }

    public boolean isLinkExpiredForThumbnail(PatientImage image) {
        String bucket = "temp." + funcclass.getS3BucketName();
        String s3Key = image.getS3Key_thumbnail();
        boolean isLinkExpired = false;
        if(image.getExpireDate_thumbnail() == null) {
            isLinkExpired = true;
        } else {
            Date date = new Date();
            isLinkExpired = date.after(image.getExpireDate_thumbnail());
        }
        return s3Key == null
                || !funcclass.s3clientNotEncrypted.doesObjectExist(bucket, s3Key)
                || isLinkExpired;
    }

    public File createThumbnail(final File imageFile, final String thumbnailFolder, final String imageExtension) {
        File thumbnailFile;
        BufferedImage originalBufferedImage;
        String extension = imageExtension;

        if (StringUtils.isEmpty(extension)) {
            extension = FilenameUtils.getExtension(imageFile.getName());
            if (StringUtils.isEmpty(extension)) {
                extension = DEFAULT_IMAGE_EXTENSION;
            }
        }
        extension = extension.toUpperCase();
        try {
            originalBufferedImage = ImageIO.read(imageFile);
            int thumbnailWidth = 150;
            int thumbnailHeight = (int) (1.0 * thumbnailWidth / originalBufferedImage.getWidth() * originalBufferedImage.getHeight());

            BufferedImage resizedImage = new BufferedImage(thumbnailWidth,
                    thumbnailHeight, originalBufferedImage.getType());
            Graphics2D g = resizedImage.createGraphics();

            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.drawImage(originalBufferedImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            g.dispose();

            File dir = new File(thumbnailFolder);
            dir.mkdirs();
            String thumbnailPath = thumbnailFolder + funcclass.FILE_DELIMETER + imageFile.getName();
            thumbnailFile = new File(thumbnailPath);
            try {
                ImageIO.write(resizedImage, extension, thumbnailFile);
            } catch (Exception e) {
                ImageIO.write(resizedImage, "", thumbnailFile);
            }

        } catch (Exception e) {
            logger.error("Can't create thumbnail for image", e);
            thumbnailFile = null;
        }
        return thumbnailFile;
    }

    public int getPatientImagesCount(long patientId) {
        int count = patientImageRepository.getImagesCount(patientId);
        logger.info("Count is {}", count);
        return count;
    }


    @Transactional
    public long convertImageFromOldFormatToNew(final PictureEntry pictureEntry, final List<PatientImage> patientImagesInNewFormat) {
        long imageId = -1;
        try { long doctorNumber = pictureEntry.getDocID();
            long patientNumber = pictureEntry.getPatID();
            int order = pictureEntry.getOrder();
            String oldType = pictureEntry.getType();
            String name = pictureEntry.getImgName();
            boolean primary = false;
            boolean isCephMainPresent = cephMainIsPresent(patientImagesInNewFormat);
            boolean isPrimaryPresent = primaryIsPresent(patientImagesInNewFormat);
            boolean isProfilePresent = profileIsPresent(patientImagesInNewFormat);

            PatientImage patientImage = new PatientImage();
            patientImage.setPatientNumber(patientNumber);
            patientImage.setName(name);
            patientImage.setExtension(pictureEntry.getExtension());

            PictureEntry[] typePictures = null;
            int minOrder = 0;
            if (oldType.equals("Ceph")) {
                typePictures = DBconnection.GetDBconnection().getPatientCephPictureEntries(patientNumber, doctorNumber);
                minOrder = typePictures[0].getOrder();
                for (PictureEntry picture: typePictures) {
                    if (picture.getOrder() < minOrder) {
                        minOrder = picture.getOrder();
                    }
                }
            }

            if (oldType.equals("Profile")) {
                typePictures = DBconnection.GetDBconnection().getPatientProfilePictureEntries(patientNumber, doctorNumber);
                minOrder = typePictures[0].getOrder();
                for (PictureEntry picture: typePictures) {
                    if (picture.getOrder() < minOrder) {
                        minOrder = picture.getOrder();
                    }
                }
            }
            PictureType newType;
            if (oldType.equals("Panoramic")) {
                newType = PictureType.PANORAMIC;
            } else if (oldType.equals("unspecified") || oldType.equals("Other")) {
                newType = PictureType.UNSPECIFIED;
            } else if (oldType.equals("Ceph") && minOrder == order && !isCephMainPresent) {
                newType = PictureType.CEPH_MAIN;
            } else if (oldType.equals("Ceph")) {
                newType = PictureType.CEPH_SECONDARY;
            } else if (oldType.equals("IntraOral")) {
                newType = PictureType.INTRAORAL;
            } else if (oldType.equals("Profile")) {
                newType = PictureType.PROFILE;
            } else {
                newType = PictureType.UNSPECIFIED;
            }
            patientImage.setType(newType);
            patientImage.setPrimary(false);


            String key;
            switch (oldType) {
                case funcclass.PIC_TYPE_CEPH:
                    if (order != minOrder) {
                        key = funcclass.getS3OutDatedKey(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH, order);
                    } else {
                        key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH);
                        primary = true;
                    }
                    break;

                case funcclass.PIC_TYPE_PROFILE:
                    if(order != minOrder) {
                        key = funcclass.getS3OutDatedKey(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE, order);
                    } else {
                        key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE);
                        primary = true;
                    }
                    break;

                case funcclass.PIC_TYPE_PANORAMIC:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PANORAMIC);
                    break;

                case funcclass.PIC_TYPE_INTRAORAL:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_INTRAORAL);
                    break;

                case funcclass.PIC_TYPE_OTHER:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, name);
                    break;

                default:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, oldType);
                    break;
            }

            File file = funcclass.getFileFromS3ByKey(patientNumber, doctorNumber, key, oldType);

            if (!isPrimaryPresent) {
                patientImage.setPrimary(primary);
            }
            imageId = savePatientImage(patientImage);
            patientImage.setId(imageId);

            s3Service.uploadPatientImageToS3(imageId, file);
            String thumbnailPath = file.getAbsolutePath().replaceAll(file.getName(), "") + "thumbnails";
            funcclass.createDirIfNotExists(thumbnailPath);
            File thumbnailFile = createThumbnail(file, thumbnailPath, patientImage.getExtension());
            s3Service.uploadPatientImageThumbnailToS3(imageId, thumbnailFile);

            file.delete();
            thumbnailFile.delete();

            PictureService.updateConverted(pictureEntry.getId(), true, imageId);
        } catch (Exception e) {
            logger.error("Can't convert image to new format ", e);
        }

        return imageId;
    }

    @Transactional
    public long convertImageFromOldFormatToNew(final PictureEntry pictureEntry, final PatientImage patientImage) {
        long imageId = -1;
        try {
            long doctorNumber = pictureEntry.getDocID();
            long patientNumber = pictureEntry.getPatID();
            int order = pictureEntry.getOrder();
            String oldType = pictureEntry.getType();
            String name = pictureEntry.getImgName();
            boolean primary = false;

            PictureEntry[] typePictures = null;
            int minOrder = 0;
            if (oldType.equals("Ceph")) {
                typePictures = DBconnection.GetDBconnection().getPatientCephPictureEntries(patientNumber, doctorNumber);
                minOrder = typePictures[0].getOrder();
                for (PictureEntry picture: typePictures) {
                    if (picture.getOrder() < minOrder) {
                        minOrder = picture.getOrder();
                    }
                }
            }

            if (oldType.equals("Profile")) {
                typePictures = DBconnection.GetDBconnection().getPatientProfilePictureEntries(patientNumber, doctorNumber);
                minOrder = typePictures[0].getOrder();
                for (PictureEntry picture: typePictures) {
                    if (picture.getOrder() < minOrder) {
                        minOrder = picture.getOrder();
                    }
                }
            }
            PictureType newType;
            if (oldType.equals("Panoramic")) {
                newType = PictureType.PANORAMIC;
            } else if (oldType.equals("unspecified") || oldType.equals("Other")) {
                newType = PictureType.UNSPECIFIED;
            } else if (oldType.equals("Ceph") && minOrder == order) {
                newType = PictureType.CEPH_MAIN;
            } else if (oldType.equals("Ceph")) {
                newType = PictureType.CEPH_SECONDARY;
            } else if (oldType.equals("IntraOral")) {
                newType = PictureType.INTRAORAL;
            } else if (oldType.equals("Profile")) {
                newType = PictureType.PROFILE;
            } else {
                newType = PictureType.UNSPECIFIED;
            }
            patientImage.setType(newType);
            patientImage.setPrimary(false);


            String key;
            switch (oldType) {
                case funcclass.PIC_TYPE_CEPH:
                    if (order != minOrder) {
                        key = funcclass.getS3OutDatedKey(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH, order);
                    } else {
                        key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_CEPH);
                        primary = true;
                    }
                    break;

                case funcclass.PIC_TYPE_PROFILE:
                    if(order != minOrder) {
                        key = funcclass.getS3OutDatedKey(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE, order);
                    } else {
                        key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PROFILE);
                        primary = true;
                    }
                    break;

                case funcclass.PIC_TYPE_PANORAMIC:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_PANORAMIC);
                    break;

                case funcclass.PIC_TYPE_INTRAORAL:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, funcclass.PIC_TYPE_INTRAORAL);
                    break;

                case funcclass.PIC_TYPE_OTHER:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, name);
                    break;

                default:
                    key = funcclass.getS3Key(patientNumber, doctorNumber, oldType);
                    break;
            }

            File file = funcclass.getFileFromS3ByKey(patientNumber, doctorNumber, key, oldType);
            imageId = updatePatientImage(patientImage);
            patientImage.setId(imageId);
            patientImage.setExtension(pictureEntry.getExtension());

            s3Service.uploadPatientImageToS3(imageId, file);
            String thumbnailPath = file.getAbsolutePath().replaceAll(file.getName(), "") + "thumbnails";
            funcclass.createDirIfNotExists(thumbnailPath);
            File thumbnailFile = createThumbnail(file, thumbnailPath, patientImage.getExtension());
            s3Service.uploadPatientImageThumbnailToS3(imageId, thumbnailFile);

            file.delete();
            thumbnailFile.delete();

            PictureService.updateConverted(pictureEntry.getId(), true, imageId);
        } catch (Exception e) {
            logger.error("Can't convert image to new format ", e);
        }

        return imageId;
    }

    private long updatePatientImage(final PatientImage patientImage) {
        patientImageRepository.updateImage(patientImage);
        return patientImage.getId();
    }

    private boolean primaryIsPresent(final List<PatientImage> patientImagesInNewFormat) {
        boolean isPresent = false;
        final Iterator<PatientImage> it = patientImagesInNewFormat.iterator();
        while ((!isPresent) && it.hasNext()) {
            isPresent = it.next().isPrimary();
        }
        return isPresent;
    }

    private boolean profileIsPresent(final List<PatientImage> patientImagesInNewFormat) {
        boolean isPresent = false;
        final Iterator<PatientImage> it = patientImagesInNewFormat.iterator();
        while ((!isPresent) && it.hasNext()) {
            isPresent = it.next().getType().equals(PictureType.PROFILE);
        }
        return isPresent;
    }



    private boolean cephMainIsPresent(final List<PatientImage> patientImagesInNewFormat) {
        boolean isPresent = false;
        final Iterator<PatientImage> it = patientImagesInNewFormat.iterator();
        while ((!isPresent) && it.hasNext()) {
            isPresent = it.next().getType().equals(PictureType.CEPH_MAIN);
        }
        return isPresent;
    }

    @Transactional
    public void uploadImage(long patientNumber, String strType, boolean isPrimary, MultipartFile imageData,
                                     Cdoctor doctor)throws IOException {

        File image = null;
        File thumbnailImage = null;
        try{

            PictureType type = PictureType.valueOf(strType);
            long doctorNumber = doctor.DocNum();
            String tempDir = funcclass.tempPatientImagesPath  + "doc_" + doctorNumber;
            funcclass.createDirIfNotExists(tempDir + "/thumbnails");
            image =new File(tempDir, imageData.getOriginalFilename());
//            image.createNewFile();
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(image));
            outputStream.write(imageData.getBytes());
            outputStream.flush();
            outputStream.close();
            logger.info("Image {} successfully created", image.getAbsolutePath());
            thumbnailImage = createThumbnail(image, tempDir + "/thumbnails", FileUtility.getExtension(image.getName()));
            logger.info("Thumbnail {} successfully created", thumbnailImage.getAbsolutePath());

            PatientImage patientImage = new PatientImage();
            patientImage.setPrimary(isPrimary);
            patientImage.setType(type);
            patientImage.setPatientNumber(patientNumber);
            patientImage.setExtension(FileUtility.getExtension(image.getName()));

            String imageName = image.getName();
            if (imageName.length() > 40) {
                String extension = FilenameUtils.getExtension(imageName);
                imageName = imageName.substring(0, 36) + "." + extension;
            }

            patientImage.setName(imageName);
            if (isPrimary) {
                long imageId = getPrimaryImageIdForType(type, patientNumber);
                if (imageId > 0) {
                    if (image.delete()) {
                        logger.info("Image {} successfully deleted", image.getAbsolutePath());
                    }

                    if (thumbnailImage.delete()) {
                        logger.info("Thumbnail image {} successfully deleted", image.getAbsolutePath());
                    }
                    throw new DublicatePrimaryImageException(imageId);
                }
            }
            if (type.equals(PictureType.PROFILE)) {
                final List<PatientImage> patientImages = patientImageRepository.getImagesForPatient(patientNumber);
                for (PatientImage patImage : patientImages) {
                    if (patImage.getType().equals(PictureType.PROFILE)) {
                        deletePatientImage(patImage.getId());
                    }
                }
                PatientService.updatePatientOwner(patientNumber, funcclass.OPERATOR);
            }
            long imageId = saveImageFromUploader(patientImage, doctor);
            patientImage.setId(imageId);
            s3Service.uploadPatientImageToS3(patientImage.getId(), image);
            s3Service.uploadPatientImageThumbnailToS3(patientImage.getId(), thumbnailImage);
            if (type.equals(PictureType.PROFILE)) {
                final String key = funcclass.getS3Key(patientNumber, DBconnection.GetDBconnection().getDocIdByPatient(patientNumber), "Profile");
                funcclass.s3client.putObject(funcclass.getS3BucketName(), key, image);
                PictureEntry pictureEntry = PictureService.getPatientPictureByName("Profile", patientNumber);
                if (pictureEntry==null) {
                    pictureEntry = new PictureEntry();
                    pictureEntry.setImgName("Profile");
                    pictureEntry.setExtension(patientImage.getExtension());
                    pictureEntry.setType("Profile");
                    pictureEntry.setDocID(doctorNumber);
                    pictureEntry.setPatID(patientImage.getPatientNumber());
                    pictureEntry.setOrder(DBconnection.GetDBconnection().getPictureOrderNum(patientNumber)+1);
                    PictureService.insertPicture(pictureEntry);
                    pictureEntry = PictureService.getPatientPictureByName("Profile", patientNumber);
                }
                if (pictureEntry!=null) {
                    PictureService.updateConverted(pictureEntry.getId(), true, imageId);
                    PictureService.updateS3Expiration(pictureEntry.getId(), null, null);
                }

            }
        }
        catch (IOException ex){
            throw new IOException();
        }
       finally {
            if (image.delete()) {
                logger.info("Image {} successfully deleted", image.getAbsolutePath());
            }

            if (thumbnailImage.delete()) {
                logger.info("Thumbnail image {} successfully deleted", thumbnailImage.getAbsolutePath());
            }
        }

    }
    @Transactional
    public void uploadImageFromDicom(long patientNumber, String strType, boolean isPrimary, String imageS3Path,
                                     BasicDoc docInfo, long docId)throws IOException {

        File imageData = null;
        File thumbnailImage = null;

        File directory =null;
        File file = null;
        try{

            PictureType type = PictureType.valueOf(strType);
//            String tempDir = funcclass.tempPatientImagesPath;
            String tempDir = funcclass.tempPatientImagesPath  + "doc_" + docId;
//            funcclass.createDirIfNotExists(tempDir + "/thumbnails");
            directory = new File(funcclass.tempPatientImagesPath  + "doc_" + docId );
            directory.mkdir();
            file = new File(/*tempDir + "/dicomDir" + patientId*/ directory.getAbsolutePath() + "/" + imageS3Path.replace("FA/",""));
//
            imageData = s3Service.downloadStlFromS3(imageS3Path, file.toPath());
//
            logger.info("Image {} successfully created", file.getAbsolutePath());
            thumbnailImage = createThumbnail(imageData, tempDir + "/thumbnails", FileUtility.getExtension(file.getName()));
            logger.info("Thumbnail {} successfully created", thumbnailImage.getAbsolutePath());

            PatientImage patientImage = new PatientImage();
            patientImage.setPrimary(isPrimary);
            patientImage.setType(type);
            patientImage.setPatientNumber(patientNumber);
            patientImage.setExtension(FileUtility.getExtension(file.getName()));

            String imageName = file.getName();
            if (imageName.length() > 40) {
                String extension = FilenameUtils.getExtension(imageName);
                imageName = imageName.substring(0, 36) + "." + extension;
            }

            patientImage.setS3Path(imageS3Path);

            patientImage.setName(imageName);
            if (isPrimary) {
                long imageId = getPrimaryImageIdForType(type, patientNumber);
                if (imageId > 0) {
                    if (file.delete()) {
                        logger.info("Image {} successfully deleted", file.getAbsolutePath());
                    }

                    if (thumbnailImage.delete()) {
                        logger.info("Thumbnail image {} successfully deleted", file.getAbsolutePath());
                    }
                    throw new DublicatePrimaryImageException(imageId);
                }
            }

            long imageId = saveImageFromUploaderWithDocInfo(patientImage, docInfo);
            patientImage.setId(imageId);
            s3Service.uploadPatientImageToS3(patientImage.getId(), file);
            s3Service.uploadPatientImageThumbnailToS3(patientImage.getId(), thumbnailImage);

        }
        catch (IOException ex){
            throw new IOException();
        }
        catch (DublicatePrimaryImageException ex) {
            logger.error(ex);
        }
        finally {
//            FileUtils.deleteDirectory(directory);
            if (directory != null) {
                FileUtils.deleteDirectory(directory);
//                for (File f : directory.listFiles()){
//                    f.delete();
//                }
//                directory.delete();
                logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
            }
            if (file != null) {
                file.delete();
                logger.info("Image {} successfully deleted", file.getAbsolutePath());
            }

            if (thumbnailImage != null) {
                thumbnailImage.delete();
                logger.info("Thumbnail image {} successfully deleted", thumbnailImage.getAbsolutePath());
            }
        }

    }

    public void saveImageChanges(HttpServletRequest request)throws IOException {

        String img64 = request.getParameter("imageData");
        long imageId = Long.valueOf(request.getParameter("imageId"));
        File tempFile = File.createTempFile(new Date().getTime() + imageId + "", new Date().getTime() + "");
        PatientImage image = getPatientImage(imageId);
        img64 = img64.replace(' ', '+');
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] decodedBytes = decoder.decodeBuffer(img64);
        ImageConverter.JPEGImageCreator(decodedBytes, tempFile.getPath());

        PictureEntry oldFormatPicture = PictureService.getPictureByNewId(imageId);
        if (oldFormatPicture != null) {
            switch (oldFormatPicture.getType()) {
                case funcclass.PIC_TYPE_CEPH:
                    funcclass.uploadFileToS3(Paths.get(tempFile.getPath()), funcclass.getS3Key(oldFormatPicture.getPatID(),
                            oldFormatPicture.getDocID(), funcclass.PIC_TYPE_CEPH));
                    break;

                case funcclass.PIC_TYPE_PROFILE:
                    funcclass.uploadFileToS3(Paths.get(tempFile.getPath()), funcclass.getS3Key(oldFormatPicture.getPatID(),
                            oldFormatPicture.getDocID(), funcclass.PIC_TYPE_PROFILE));
                    break;

                case funcclass.PIC_TYPE_PANORAMIC:
                    funcclass.uploadFileToS3(Paths.get(tempFile.getPath()), funcclass.getS3Key(oldFormatPicture.getPatID(),
                            oldFormatPicture.getDocID(), funcclass.PIC_TYPE_PANORAMIC));
                    break;

                case funcclass.PIC_TYPE_INTRAORAL:
                    funcclass.uploadFileToS3(Paths.get(tempFile.getPath()), funcclass.getS3Key(oldFormatPicture.getPatID(),
                            oldFormatPicture.getDocID(), funcclass.PIC_TYPE_INTRAORAL));
                    break;

                case funcclass.PIC_TYPE_OTHER:
                    funcclass.uploadFileToS3(Paths.get(tempFile.getPath()), funcclass.getS3Key(oldFormatPicture.getPatID(),
                            oldFormatPicture.getDocID(), oldFormatPicture.getImgName()));
                    break;
            }
            PictureService.updateS3Expiration(oldFormatPicture.getId(), "no_key", new Date());

        }

        s3Service.uploadPatientImageToS3(imageId, tempFile);
        Date date = funcclass.getNextDayAfterDate();
        String s3Key = s3Service.uploadToTempBucket(tempFile);
        image.setS3Key_image(s3Key);
        image.setExpireDate_image(date);
        updateExpirationForImage(image);

        String thumbnailPath = tempFile.getAbsolutePath().replaceAll(tempFile.getName(), "") + "thumbnails";
        funcclass.createDirIfNotExists(thumbnailPath);
        File thumbnail = createThumbnail(tempFile, thumbnailPath, image.getExtension());
        s3Service.uploadPatientImageThumbnailToS3(imageId, thumbnail);
        date = funcclass.getNextDayAfterDate();
        s3Key = s3Service.uploadToTempBucket(thumbnail);
        image.setS3Key_thumbnail(s3Key);
        image.setExpireDate_thumbnail(date);
        updateExpirationForThumbnail(image);

        tempFile.delete();
        thumbnail.delete();
    }

    public void createUnspecifiedImage(HttpServletRequest request, Cdoctor doctor) throws IOException {

        String img64 = request.getParameter("image");
        long patientId = Long.valueOf(request.getParameter("patientId"));

        img64 = img64.replace(' ', '+');
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] decodedBytes = decoder.decodeBuffer(img64);
        String tempDir = funcclass.tempPatientImagesPath  + "doc_" + doctor.DocNum();
        funcclass.createDirIfNotExists(tempDir + "/thumbnails");
        File imageFile = new File(tempDir + funcclass.FILE_DELIMETER + "UNSPECIFIED.jpeg");
        ImageConverter.JPEGImageCreator(decodedBytes, imageFile.getPath());
        File thumbnailFile = createThumbnail(imageFile, tempDir + "/thumbnails", FileUtility.getExtension(imageFile.getName()));

        PatientImage patientImage = new PatientImage();
        patientImage.setPrimary(false);
        patientImage.setType(PictureType.UNSPECIFIED);
        patientImage.setPatientNumber(patientId);
        String imageName = imageFile.getName();
        if (imageName.length() > 40) {
            String extension = FilenameUtils.getExtension(imageName);
            imageName = imageName.substring(0, 36) + "." + extension;
        }

        patientImage.setName(imageName);

        patientImage.setName(imageName);
        long imageId = savePatientImage(patientImage);
        patientImage.setId(imageId);


        s3Service.uploadPatientImageToS3(imageId, imageFile);
        s3Service.uploadPatientImageThumbnailToS3(imageId, thumbnailFile);

        imageFile.delete();
        thumbnailFile.delete();
    }
}
