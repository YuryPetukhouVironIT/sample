package com.cephx.def.service;

import com.cephx.def.BasicPatientInfoData;
import com.cephx.def.DBconnection;
import com.cephx.def.dto.stl.StlJson;
import com.cephx.def.dto.stl.StlJsonCreator;
import com.cephx.def.funcclass;
import com.cephx.def.intercom.IntercomClient;
import com.cephx.def.service.db.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipCreationTask implements Runnable {

    private static final String INTERCOM_MESSAGE_BODY = "Processing completed for patient %1$s %2$s, please refresh the patients list and click the 'Actions' menu in the 3D column to access segmentation and reports";
    private static final String INTERCOM_HTML_MESSAGE_BODY = "Dear %1$s <br> Your segmented STL for patient %2$s %3$s is complete.  <br> Please login to your cephX account, select your patient and click the 'Actions' menu under the 3D column to download or view your STL images and movies. <br><br> Click here to go to your account: https://www.cephx.com <br><br> Thanks, <br> The cephX team";
    private static final String INTERCOM_EMAIL_SUBJECT = "Your segmented CBCT is ready";
    private static final Logger logger = LogManager.getLogger(ZipCreationTask.class);
    private static final DBconnection dbConnection = DBconnection.GetDBconnection();
    private static final Object lockObject = new Object();

    private final S3Service s3Service;
    private final IntercomClient intercomClient;
    private final String stl1S3Path;
    private final long patientId;
    private final long doctorId;
    private final String username;
    private final String doctorAddress;
    private final String patientFirstName;
    private final String patientLastName;
    private final boolean isCompany;

    public ZipCreationTask(final S3Service s3Service, final IntercomClient intercomClient, final String stl1S3Path, final long patientId, final String doctorAddress, final long doctorId, final String patientFirstName, final String patientLastName, final String username, final boolean isCompany) {
        this.s3Service = s3Service;
        this.intercomClient = intercomClient;
        this.stl1S3Path = stl1S3Path;
        this.patientId = patientId;
        this.doctorAddress = doctorAddress;
        this.doctorId = doctorId;
        this.patientFirstName = patientFirstName;
        this.patientLastName = patientLastName;
        this.username = username;
        this.isCompany = isCompany;
    }

    @Override
    public void run() {
        synchronized (lockObject) {
            try {
                if (!PatientService.isZipStlExist(patientId)) {
                    createStlZip(patientId, stl1S3Path);
                }
            } catch (Exception e) {
                logger.error("Failed to create zip", e);
            }
            try {
                if (StringUtils.isBlank(PatientService.stlJsonPath(patientId))) {
                    createStlZipJson(patientId, stl1S3Path, isCompany);
                }
            } catch (Exception e) {
                logger.error("Failed to create json for viewer", e);
            }
            try {
                if (!PatientService.isZipVideoExist(patientId)) {
                    createVideoZip(patientId, stl1S3Path);
                }
            } catch (RuntimeException e) {
                logger.error("Failed to create zip video", e);
            }
            if (intercomClient != null) {
                sendIntercomNotification();
            }
        }
    }

    public void createStlZipJson(final long patientId, final String stl1S3Path, final boolean isCompany) throws Exception {
        String mobileZipName = getZipNameIfPresent("mobile.zip", stl1S3Path);
        String desktopZipName = getZipNameIfPresent("desktop.zip", stl1S3Path);
        if (zipExceedsMaximumSize(stl1S3Path, desktopZipName)) {
            desktopZipName = mobileZipName;
        }
        final String jsonFileName = "zipinput.json";
        final String s3Key = stl1S3Path + "/" + jsonFileName;
        final String tempDir = funcclass.tempPatientImagesPath;
        final File directory = new File(tempDir + "/dicomDirJson" + patientId);
        directory.mkdirs();
        final File jsonFile = new File(directory.getAbsolutePath() + "/" + jsonFileName);
        final String readOnlyGuid = PatientService.createStlReadOnlyGuid(patientId);
        final String readWriteGuid = PatientService.createStlReadWriteGuid(patientId);
        final StlJson stlJson = new StlJsonCreator(patientId, patientFirstName, patientLastName, isCompany, PatientService.getStlPath(patientId), s3Service, getZipFileUrl(patientId, mobileZipName), getZipFileUrl(patientId, desktopZipName), lastKeyPart(mobileZipName)).createStlJson();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(jsonFile, stlJson);
        s3Service.uploadDicomFileToS3(s3Key, jsonFile);
        jsonFile.delete();
        directory.delete();
        dbConnection.setAnalysisJsonPath(patientId, getZipFileUrl(patientId, jsonFileName));
    }

    private boolean zipExceedsMaximumSize(final String stl1S3Path, final String desktopZipName) {
        final long fileSize = s3Service.s3FileSize(stl1S3Path + "/" + desktopZipName);
        return fileSize > funcclass.MAX_DICOM_ZIP_SIZE;
    }

    private String getZipNameIfPresent(final String matchingZipName, final String stl1S3Path) {
        String zipFileName = matchingZipName;
        String zipFileKey = stl1S3Path + "/" + zipFileName;
        if (!s3Service.s3FileExists(zipFileKey)) {
            zipFileName = "stl_" + lastKeyPart(stl1S3Path) + ".zip";
            zipFileKey = stl1S3Path + "/" + zipFileName;
            if (!s3Service.s3FileExists(zipFileKey)) {
                createStlZip(patientId, stl1S3Path);
                if (!s3Service.s3FileExists(zipFileKey)) {
                    throw new RuntimeException("Failed to find or create zip file");
                }
            }
        }
        return zipFileName;
    }

    private String getZipFileUrl(final Long patientId, final String zipFileName) {
        return funcclass.s3UrlDirect+PatientService.getStlPath(Long.valueOf(patientId))+"/"+zipFileName;
    }

    private void createStlZip(final long patientId, final String stl1S3Path) {
        final String patientZipName = "stl_" + lastKeyPart(stl1S3Path) + ".zip";
        dbConnection.setPatZipStl(patientId, createZip(patientId, stl1S3Path, patientZipName, "STL"));
    }

    private void createVideoZip(final long patientId,final String stl1S3Path) {
        final String patientZipName = "video_" + lastKeyPart(stl1S3Path) + ".zip";
        dbConnection.setPatZipVideo(patientId, createZip(patientId, stl1S3Path, patientZipName, "MP4"));
    }


    private boolean createZip(final long patientId,final String stl1S3Path, final String patientZipName, final String extenstion) {
        boolean fileExists = false;
        final String tempDir = funcclass.tempPatientImagesPath;
        final File directory = new File(tempDir + "/dicomDir" + patientId);
        directory.mkdirs();
        final java.util.List<String> stlPaths = new ArrayList<>();
        stlPaths.add(stl1S3Path);
        final java.util.List<File> patientStlFiles = downloadPatientFilesFromS3Directory(stlPaths, directory, extenstion);
        File zipFile = null;
        if (!patientStlFiles.isEmpty()) {
            zipFile = zipFiles(patientStlFiles, patientZipName);
            final String s3Key = stl1S3Path + "/" + patientZipName;
            s3Service.uploadDicomFileToS3(s3Key, zipFile);
            fileExists = s3Service.s3FileExists(s3Key);
            if (!fileExists) {
                throw new RuntimeException("Failed to create zip for stlPath "+ stl1S3Path);
            }
        }
        cleanFiles(patientStlFiles, directory, zipFile);
        return fileExists;
    }


    private String lastKeyPart(final String s3Path) {
        String[] pathSubstrings= s3Path.split("/");
        return pathSubstrings[pathSubstrings.length-1];
    }

    private java.util.List<File> downloadPatientFilesFromS3Directory(final List<String> stlPaths, final File directory, final String extenstion) {
        final java.util.List<File> patientStlFiles = new ArrayList<>();
        for (String stlPath : stlPaths) {
            final List<String> fileS3Paths = s3Service.getPathesFromDirectory(stlPath);
            downloadS3Files(directory, patientStlFiles, fileS3Paths, extenstion.toUpperCase());
        }
        return patientStlFiles;
    }

    private void downloadS3Files(final File directory, final List<File> patientStlFiles, final List<String> fileS3Paths, final String extension) {
        for (String fileS3Path : fileS3Paths) {
            if (fileS3Path.toUpperCase().trim().endsWith("."+extension.toUpperCase())) {
                final String fileName = lastKeyPart(fileS3Path);
                final File destinationFile = new File(directory.getAbsolutePath()+"/"+fileName);
                final File stlFile = s3Service.downloadSingleFileFromS3(fileS3Path, destinationFile.toPath());
                final File stlFileWithExtenstion = new File(directory.getParentFile().getAbsolutePath() + "/" + fileName);
                stlFile.renameTo(stlFileWithExtenstion);
                patientStlFiles.add(stlFileWithExtenstion);
            }
        }
    }

    private File zipFiles(final List<File> patientFiles, final String patientName) {
        final File zipFile = new File (patientName);
        try {
            logger.info("Starting zipping to "+zipFile.getAbsolutePath());
            zipFile.createNewFile();
            final FileOutputStream out = new FileOutputStream(patientName);
            final ZipOutputStream zipOut = new ZipOutputStream(out);
            for (File stlDir : patientFiles) {
                if (stlDir.isDirectory()) {
                    for (File stlFile : stlDir.listFiles()) {
                        zipStlFile (stlFile, zipOut);
                    }
                } else {
                    zipStlFile(stlDir, zipOut);
                }
            }
            zipOut.close();
            out.close();
        } catch (IOException ex) {
            logger.error(ex);
        }
        return zipFile;
    }

    private void zipStlFile(final File stlFile, final ZipOutputStream zipOut) throws IOException {
        logger.info("Zipping started for "+stlFile.getAbsolutePath());
        final FileInputStream fileInputStream = new FileInputStream(stlFile);
        final ZipEntry zipEntry = new ZipEntry(stlFile.getName());
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while ((length = fileInputStream.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fileInputStream.close();
        logger.info("Zipping completed for "+stlFile.getAbsolutePath());
    }

    private void cleanFiles(final java.util.List<File> patientStlFiles, final File directory, final File zipFile) {
        if (directory != null) {
            for (File stlFile : patientStlFiles) {
                if (stlFile != null) {
                    stlFile.delete();
                    logger.info("Files {} successfully deleted ", directory.getAbsolutePath());
                }
            }
        }
        if (zipFile!=null) {
            zipFile.delete();
        }
        if (directory != null) {
            directory.delete();
            logger.info("Directory {} successfully deleted ", directory.getAbsolutePath());
        }
    }

    private void sendIntercomNotification() {
        final String messageBody = String.format(INTERCOM_MESSAGE_BODY, patientFirstName, patientLastName);
        intercomClient.sendMessage(doctorAddress, messageBody);
        final String htmlBody = String.format(INTERCOM_HTML_MESSAGE_BODY, username, patientFirstName, patientLastName);
        final String subject = INTERCOM_EMAIL_SUBJECT;
        intercomClient.sendEmail(doctorAddress, subject, htmlBody);
    }

}
