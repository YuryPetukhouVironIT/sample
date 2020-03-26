package com.cephx.def.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.cephx.def.BasicDoc;
import com.cephx.def.DBconnection;
import com.cephx.def.SlackClient;
import com.cephx.def.funcclass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class FileUploadTask implements Runnable {

    private static final Logger logger = LogManager.getLogger(FileUploadTask.class);
    private static final int MAX_UPLOAD_ATTEMPTS = 20;

    private final File file;
    private final long patientId;
    private String prefix = "";

    public FileUploadTask(final long patientId, final File file) {
        this.patientId = patientId;
        this.file = file;
    }

    public FileUploadTask(final long patientId, final File file, final String prefix) {
        this.patientId = patientId;
        this.file = file;
        this.prefix = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    @Override
    public void run() {
        final String bucket = funcclass.getS3RawBucketName();
        final String patientIdString = String.valueOf(patientId);
        final String fileCopyName = file.getName();
        final String uploadFileName = fileCopyName.length() > patientIdString.length() ? fileCopyName.substring(patientIdString.length()) : fileCopyName;
        String key = "pat_" + String.valueOf(patientId) + "/" + prefix + uploadFileName;
        key = key.replace("//", "/");
        int attempts = 0;
        boolean fileUploaded = false;
        while (!fileUploaded && attempts < MAX_UPLOAD_ATTEMPTS) {
            try {
                final PutObjectResult putObjectResult = funcclass.s3clientNotEncrypted.putObject(bucket, key, file);
                logger.info("File {} was uploaded to bucket {} with key {}", file.getName(), bucket, key);
                fileUploaded = true;
                file.delete();
                logger.info("File {} was deleted", file.getAbsolutePath());
            } catch (AmazonServiceException e) {
                logger.error("Failed to upload raw file {} ", file.getAbsolutePath(), e);
                attempts++;
            }
        }
        if (attempts >= MAX_UPLOAD_ATTEMPTS) {
            logger.error("Failed to upload raw file {} in {} attempts ", file.getAbsolutePath(), attempts);
            try {
                slackNotifyUploadFailure();
            } catch (Exception e) {
                logger.error("Failed to send slack notification on upload to raw storage for patient " + patientId, e);
            }

        }
    }

    private void slackNotifyUploadFailure() throws Exception {
        if(!funcclass.isLocalHost()){
            long docId = DBconnection.GetDBconnection().getDocIdByPatient(patientId);
            BasicDoc docInfo = DBconnection.GetDBconnection().getDocInfo(docId);
            StringBuilder slackMessage = new StringBuilder();
            slackMessage.append("Failed to upload file to raw storage:\n");
            slackMessage.append("No:").append(docInfo.docnum).append("\n");
            slackMessage.append("Name:").append(docInfo.getDocFullName()).append("\n");
            slackMessage.append("Email:").append(docInfo.email).append("\n");
            slackMessage.append("File path:").append(file.getAbsolutePath()).append("\n");
            slackMessage.append("PatientID:").append(patientId).append("\n");
            slackMessage.append("Env:").append(funcclass.getEnvironment()).append("\n");
            SlackClient.sendMessageToUrl(slackMessage.toString(), SlackClient.urlForDicom);

        }
    }

}
