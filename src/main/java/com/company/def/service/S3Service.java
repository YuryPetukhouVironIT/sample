package com.cephx.def.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.CreateInvalidationResult;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.cephx.def.DBconnection;
import com.cephx.def.funcclass;
import com.cephx.def.model.PatientImage;
import com.cephx.def.service.db.PatientImageService;
import com.cephx.def.struct.struct.PictureEntry;
import com.cephx.def.util.file.FileUtility;
import com.cephx.def.util.string.RandomString;
import com.cephx.def.util.string.StringUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class S3Service {
    private static final Logger logger = LogManager.getLogger(S3Service.class);

    @Autowired
    private PatientImageService patientImageService;

    public void uploadPatientImageToS3(long imageId, File imageFile) {
        String bucket = funcclass.getS3BucketName();
        String key = getS3KeyForPatientImage(imageId);
        funcclass.s3client.putObject(bucket, key, imageFile);
        logger.info("Image {} was uploaded to bucket {} with key {}", imageId, bucket, key);
    }

    public void moveDicomToS3Glacier(final String s3Key) {
        final Runnable moveToGlacierTask = new MoveToGlacierTask(s3Key);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(moveToGlacierTask);
    }


    public List<String> getPathesFromDirectory(String prefix) {
        String bucket = funcclass.getS3BucketName();
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix)/*.withDelimiter("/")*/;
        ListObjectsV2Result listing = funcclass.s3clientNotEncrypted.listObjectsV2(req);
        List<String> pathes = new ArrayList<>();
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            pathes.add(summary.getKey());
        }
        if (pathes.size() > 0) {
            pathes.remove(0);
        }
        return pathes;
    }

    public void uploadDicomDirectoryToS3(final String key, final File dicom) throws IOException, InterruptedException {
        final String bucket = funcclass.getS3BucketName();
        final TransferManager transfer = new TransferManager();
        final File[] filesInFolder =  dicom.listFiles();
        for (int i=0; i<filesInFolder.length; i++) {
            final File file = filesInFolder[i];
            if (!StringUtility.isLatinString(file.getName())) {
                final File destination = new File(dicom.getAbsolutePath()+"/"+ FileUtility.getFileItemName(file.getName(),i));
                destination.createNewFile();
                final String rarCommand = " mv " + "\"" + file.getAbsolutePath() + "\"" + " " + "\"" + destination.getAbsolutePath() + "\"";
                final Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", rarCommand, "exit"});
                try (final InputStream stdin = proc.getInputStream();
                     final InputStreamReader isr = new InputStreamReader(stdin);
                     final BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                    proc.waitFor();
                }

            }
        }
        final MultipleFileUpload uploader = transfer.uploadDirectory(bucket, key, dicom, true);
        makeDicomUpload(uploader);
        logger.info("Directory {} was uploaded to bucket {} with key {}", dicom.getName(), bucket, key);
        transfer.shutdownNow();
    }

    public void uploadDicomFileToS3(final String key, final File dicom) {
        final String bucket = funcclass.getS3BucketName();
        final TransferManager transfer = new TransferManager();
        final Upload upload = transfer.upload(bucket, key, dicom);
        makeDicomUpload(upload);
        logger.info("Image {} was uploaded to bucket {} with key {}", dicom.getName(), bucket, key);
        transfer.shutdownNow();
    }

    public void uploadPublicDicomFileToS3(String key, File dicom) {
        final String bucket = funcclass.getS3BucketName();
        final TransferManager transfer = new TransferManager();
        if (funcclass.s3client.doesObjectExist(bucket,key)) {
            funcclass.s3client.deleteObject(bucket,key);
        }
        final PutObjectRequest request = new PutObjectRequest(bucket, key, dicom).withCannedAcl(CannedAccessControlList.PublicRead);
        final Upload upload = transfer.upload(request);
        makeDicomUpload(upload);
        logger.info("saved.json {} was updated at bucket {} with key {}", dicom.getName(), bucket, key);
        transfer.shutdownNow();
    }

    private void makeDicomUpload(final Transfer upload) {
        try {
            upload.waitForCompletion();
        } catch (AmazonServiceException e) {
            logger.error("Amazon service error: " + e.getMessage());
        } catch (AmazonClientException e) {
            logger.error("Amazon client error: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Transfer interupted: " + e.getMessage());
        }
    }

    public void uploadPatientImageThumbnailToS3(long imageId, File thumbnailFile) {
        String bucket = funcclass.getS3BucketName();
        String key = getS3KeyForPatientImageThumbnail(imageId);
        funcclass.s3client.putObject(bucket, key, thumbnailFile);
        logger.info("Thumbnail {} was uploaded to bucket {} with key {}", imageId, bucket, key);
    }

    public String uploadScreenshotToS3(long screenshotId, File screenshotFile) {
        String bucket = funcclass.getS3BucketName();
        String key = getS3KeyForScreenshot(screenshotId);
        funcclass.s3client.putObject(bucket, key, screenshotFile);
        logger.info("Screenshot {} was uploaded to bucket {} with key {}", screenshotId, bucket, key);
        return key;
    }

    public String uploadToTempBucket(File file) {
        String easy = RandomString.digits + "ACEFGHJKLMNPQRUVWXYabcdefhijkprstuvwx";
        RandomString randomString = new RandomString(64, new SecureRandom(), easy);
        String s3Key = randomString.nextString();

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("image/png");
        PutObjectRequest putObjectRequest = new PutObjectRequest("temp." + funcclass.getS3BucketName(),
            s3Key, file)
            .withCannedAcl(CannedAccessControlList.PublicRead)
            .withMetadata(objectMetadata);

        funcclass.s3clientNotEncrypted.putObject(putObjectRequest);
        return s3Key;
    }

    public String getS3UrlForPatientImage(PatientImage image) throws IOException {
        if (image.getS3Key_image() == null) {
            image = patientImageService.getPatientImage(image.getId());
        }
        updateImageExpiration(image);
        return funcclass.s3UrlTemp + image.getS3Key_image();
    }

    public void updateImageExpiration(PatientImage image) throws IOException {
        if (patientImageService.isLinkExpiredForImage(image)) {
            File file = getPatientImageFileFromS3(image);
            Date date = funcclass.getNextDayAfterDate();
            if (file != null && file.exists()) {
                String s3Key = uploadToTempBucket(file);
                file.delete();
                image.setS3Key_image(s3Key);
                image.setExpireDate_image(date);
                patientImageService.updateExpirationForImage(image);
            }

        }
    }

    public String getS3UrlForPatientImageThumbnail(PatientImage image) throws IOException {
        if (image.getS3Key_thumbnail() == null) {
            image = patientImageService.getPatientImage(image.getId());
        }
        updateThumbnailExpiration(image);
        return funcclass.s3UrlTemp + image.getS3Key_thumbnail();
    }

    public void updateThumbnailExpiration(PatientImage image) throws IOException {
        if (patientImageService.isLinkExpiredForThumbnail(image)) {
            File file = getPatientImageThumbnailFileFromS3(image);
            Date date = funcclass.getNextDayAfterDate();
            if (file != null && file.exists()) {
                String s3Key = uploadToTempBucket(file);
                file.delete();
                image.setS3Key_thumbnail(s3Key);
                image.setExpireDate_thumbnail(date);

                patientImageService.updateExpirationForThumbnail(image);
            }
        }
    }

    public File getPatientImageFileFromS3(PatientImage image) throws IOException {
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), getS3KeyForPatientImage(image.getId()))) {
            return getFileFromS3(getS3KeyForPatientImage(image.getId()), getPathForPatientImage(image));
        } else {
            if (image.getS3Path() != null) {
                return getFileFromS3Images(image.getS3Path(), getPathForPatientImage(image));
            } else {
                final long doctorNumber = DBconnection.GetDBconnection().getPatientBasicInfo(image.getPatientNumber()).getDocId();
                final PictureEntry pictureEntry = DBconnection.GetDBconnection().getPictureEntryByNewId(image.getId());
                final String oldType = pictureEntry.getType();
                final String key = funcclass.getS3Key(image.getPatientNumber(), doctorNumber, oldType);
                return funcclass.getFileFromS3ByKey(image.getPatientNumber(), doctorNumber, key, oldType);
            }
        }
    }

    public File downloadStlFromS3(String s3key, Path path) throws IOException {

        return getFileFromS3(s3key, path);

    }

    public File downloadSingleFileFromS3(String s3key, Path path) {

        return getSingleFileFromS3(s3key, path);

    }


    public File downloadJPGFromS3(String s3key, Path path) throws IOException {

        return getFileFromS3(s3key, path);

    }

    public File getPdfFromS3(String s3key, Path path) throws IOException {

        String bucket = funcclass.getS3BucketName();
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
            S3Object obj = funcclass.s3client.getObject(funcclass.getS3BucketName(), s3key);
            InputStream inputStream = obj.getObjectContent();

            File file = path.toFile();
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
        }
        return path.toFile();
//        return getFileFromS3(s3key, path);
    }

    public File getStlFromS3(String s3key, Path path) throws IOException {

        String bucket = funcclass.getS3BucketName();
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
            S3Object obj = funcclass.s3client.getObject(funcclass.getS3BucketName(), s3key);
            InputStream inputStream = obj.getObjectContent();

            File file = path.toFile();
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
            outputStream.close();
        }
        return path.toFile();
//        return getFileFromS3(s3key, path);
    }

    private File getFileFromS3Images(String s3key, Path path) {
        String bucket = funcclass.getS3BucketName();
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
            S3Object obj = funcclass.s3client.getObject(funcclass.getS3BucketName(), s3key);
            InputStream inputStream = obj.getObjectContent();
            int maxTries = 3;
            int count = 0;
            boolean stop = false;
            while (!stop) {
                try {
                    path.toFile().mkdirs();
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    stop = true;

                    if (!obj.getObjectMetadata().getUserMetadata().containsKey("x-amz-unencrypted-content-length")) {
                        funcclass.s3client.putObject(bucket, s3key, path.toFile());
                    }

                } catch (Exception e) {
                    logger.error("Exception in access to file " + path, e);
                    stop = count == maxTries;
                    count++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        logger.error(e1);
                    }
                }
            }
        }
        return path.toFile();
    }

    private File getFileFromS3(String s3key, Path path) {
        String bucket = funcclass.getS3BucketName();
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
            S3Object obj = funcclass.s3client.getObject(funcclass.getS3BucketName(), s3key);
            InputStream inputStream = obj.getObjectContent();
            int maxTries = 3;
            int count = 0;
            boolean stop = false;
            while (!stop) {
                try {
                    if (!path.toFile().exists()) {
                        path.toFile().mkdirs();
                    }
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    stop = true;

                    if (!obj.getObjectMetadata().getUserMetadata().containsKey("x-amz-unencrypted-content-length")) {
                        funcclass.s3client.putObject(bucket, s3key, path.toFile());
                    }

                } catch (Exception e) {
                    logger.error("Exception in access to file " + path, e);
                    stop = count == maxTries;
                    count++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        logger.error(e1);
                    }
                }
            }
        }
        return path.toFile();
    }

    private File getSingleFileFromS3(String s3key, Path path) {
        String bucket = funcclass.getS3BucketName();
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
            S3Object obj = funcclass.s3client.getObject(funcclass.getS3BucketName(), s3key);
            InputStream inputStream = obj.getObjectContent();
            int maxTries = 3;
            int count = 0;
            boolean stop = false;
            while (!stop) {
                try {
                    if (!path.toFile().getParentFile().exists()) {
                        path.toFile().getParentFile().mkdirs();
                    }
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    stop = true;
                } catch (Exception e) {
                    logger.error("Exception in access to file " + path, e);
                    stop = count == maxTries;
                    count++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        logger.error(e1);
                    }
                }
            }
            try {
                inputStream.close();
                obj.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return path.toFile();
    }


    public void removePatientImage(String s3key) {
        try {
            if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), s3key)) {
                funcclass.s3client.deleteObject(funcclass.getS3BucketName(), s3key);
                logger.info("Image was successfully removed from S3 with key {}", s3key);
            }
        } catch (Exception e) {
            logger.error("Can't remove image from S3 with key " + s3key, e);
        }
    }

    public File getPatientImageThumbnailFileFromS3(PatientImage image) throws IOException {
        if (funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), getS3KeyForPatientImageThumbnail(image.getId()))) {
            return getFileFromS3(getS3KeyForPatientImageThumbnail(image.getId()), getPathForPatientImageThumbnail(image));
//            return getFileFromS3(getS3KeyForPatientImage(image.getId()), getPathForPatientImage(image));
        } else {
            if (image.getS3Path() != null) {
                return getFileFromS3(image.getS3Path(), getPathForPatientImage(image));
            } else {
                return null;
            }
        }

    }

    public String getS3KeyForPatientImage(long imageId) {
        return "patientImage-" + imageId;
    }

    public String getS3KeyForDicom(String directoryName) {
        return "dicom-" + directoryName;
    }

    public String getS3KeyForPatientImageThumbnail(long imageId) {
        return "patientImageThumbnail-" + imageId;
    }

    public String getS3KeyForScreenshot(long screenshotId) {
        return "screenshot-" + screenshotId;
    }

    private Path getPathForPatientImage(PatientImage image) throws IOException {
        String fileName = image.getName();
        if (fileName == null) {
            fileName = image.getType().name();
        }
        if (!fileName.contains(".")) {
//            fileName += ".jpg";
        }
        String path = funcclass.patientImagesPath + image.getPatientNumber() + funcclass.FILE_DELIMETER;
        Files.createDirectories(Paths.get(path));
        path += fileName;
        return Paths.get(path);
    }

    private Path getPathForPatientImageThumbnail(PatientImage image) throws IOException {
        String fileName = image.getName();
        if (fileName == null) {
            fileName = image.getType().name();
        }
        if (!fileName.contains(".")) {
            fileName += ".jpg";
        }
        String path = funcclass.patientImagesPath + image.getPatientNumber()
            + funcclass.FILE_DELIMETER + "thumbnails" + funcclass.FILE_DELIMETER;
        Files.createDirectories(Paths.get(path));
        path += fileName;
        return Paths.get(path);
    }

    public File copyPatientImagesFromS3ToFolder(long patientNumber) throws IOException {
        List<PatientImage> patientImages = patientImageService.getPatientImages(patientNumber);
        String directory = funcclass.tempPatientImagesPath + "pat_" + patientNumber + funcclass.FILE_DELIMETER;
        funcclass.createDirIfNotExists(directory);
        File directoryFile = new File(directory);
        FileUtils.cleanDirectory(directoryFile);
        for (PatientImage patientImage : patientImages) {
            String imageUrl = getS3UrlForPatientImage(patientImage);
            String extension = FilenameUtils.getExtension(patientImage.getName());
            if (StringUtils.isEmpty(extension)) {
                extension = "png";
            }
            File imageFile = new File(directory + patientImage.getId() + "." + extension);
            FileUtils.copyURLToFile(new URL(imageUrl), imageFile);
        }
        return directoryFile;
    }

    public boolean s3FileExists(final String key) {
        return funcclass.s3client.doesObjectExist(funcclass.getS3BucketName(), key);
    }

    public long s3FileSize(final String key) {
        return funcclass.s3client.getObjectMetadata(funcclass.getS3BucketName(), key).getContentLength();
    }

    public void uploadRawFileToS3(final long patientId, final File file) throws IOException {
        uploadRawFileToS3(patientId, file, "");
    }


    public void uploadRawFileToS3(final long patientId, final File file, final String prefix) throws IOException {
        final File directory = new File(funcclass.tempUploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        final File fileCopy = new File(directory.getAbsolutePath() + "/" + patientId + file.getName());
        Files.copy(file.toPath(), fileCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        final Runnable uploadTask = new FileUploadTask(patientId, fileCopy, prefix);
        Executors.newSingleThreadExecutor().execute(uploadTask);
    }

    private void makeInvalidation(final String key) {
        if (funcclass.AWS_CLOUDFRONT_DISTRIBUTION_S3!=null) {
            final AWSCredentials awsCredentials = new ProfileCredentialsProvider().getCredentials();
            final AmazonCloudFrontClient client = new AmazonCloudFrontClient(awsCredentials);
            final com.amazonaws.services.cloudfront.model.Paths invalidation_paths = new com.amazonaws.services.cloudfront.model.Paths().withItems(key).withQuantity(1);
            final InvalidationBatch invalidationBatch = new InvalidationBatch(invalidation_paths, String.valueOf(new Date().getTime()));
            final CreateInvalidationRequest invalidation = new CreateInvalidationRequest(funcclass.AWS_CLOUDFRONT_DISTRIBUTION_S3, invalidationBatch);
            logger.info("Creating invalidation for file with key: "+key);
            final CreateInvalidationResult ret = client.createInvalidation(invalidation);
            logger.info("Invalidation completed for file with key: "+key);
        }
    }
}
