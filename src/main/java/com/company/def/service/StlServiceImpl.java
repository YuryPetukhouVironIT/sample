package com.cephx.def.service;

import com.Ostermiller.util.Base64;
import com.cephx.def.funcclass;
import com.cephx.def.service.db.DicomTaskService;
import com.cephx.def.service.db.PatientService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class StlServiceImpl implements StlService {

    private static final Logger logger = LogManager.getLogger(StlServiceImpl.class);

    @Autowired
    private DicomTaskService dicomTaskService;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private BitlyService bitlyService;

    @Override
    public String jsonLocation(final Long patientId) throws Exception {
        String jsonLocation = PatientService.stlJsonPath(patientId);
        if (StringUtils.isBlank(jsonLocation)) {
            if (StringUtils.isNotBlank(PatientService.getStlPath(patientId))) {
                dicomTaskService.createZipJsonSynchronously(patientId);
                jsonLocation = PatientService.stlJsonPath(patientId);
                bitlyService.createShortenedLinks(patientId);
            }
        }
        return jsonLocation;
    }

    @Override
    public boolean updateViewerSettings(final long patientId, final String updatedSettingsString) throws IOException {
        final String s3Path = PatientService.getStlPath(patientId);
        if (StringUtils.isBlank(s3Path)) {
            final String message = "Cannot get stl s3 path for patient " + patientId;
            throw new RuntimeException(message);
        }
        final String settingsString = Base64.decode(updatedSettingsString).trim();
        final File settingsFile = new File(funcclass.innerLogDir + patientId + "/" + funcclass.STL_VIEWER_SETTINGS_UPDATE_FILE_NAME);
        if (!settingsFile.getParentFile().exists()) {
            settingsFile.getParentFile().mkdirs();
        }
        settingsFile.createNewFile();
        try {
            Files.write(settingsFile.toPath(), settingsString.getBytes());
            s3Service.uploadRawFileToS3(patientId, settingsFile);
            final String s3Key = s3Path + "/" + funcclass.STL_VIEWER_SETTINGS_UPDATE_FILE_NAME;
            s3Service.uploadPublicDicomFileToS3(s3Key, settingsFile);
        } finally {
            settingsFile.delete();
            settingsFile.getParentFile().delete();
        }
        return true;
    }

    @Override
    public String getSavedViewerSettings(long patientId) throws IOException {
        final String s3Path = PatientService.getStlPath(patientId);
        if (StringUtils.isBlank(s3Path)) {
            final String message = "Cannot get stl s3 path for patient " + patientId;
            throw new RuntimeException(message);
        }
        final File settingsFile = new File(funcclass.innerLogDir + patientId + "/" + funcclass.STL_VIEWER_SETTINGS_UPDATE_FILE_NAME);
        if (!settingsFile.getParentFile().exists()) {
            settingsFile.getParentFile().mkdirs();
        }
        settingsFile.createNewFile();
        try {

            final String s3Key = s3Path + "/" + funcclass.STL_VIEWER_SETTINGS_UPDATE_FILE_NAME;
            if (s3Service.s3FileExists(s3Key)) {
                s3Service.downloadSingleFileFromS3(s3Key, settingsFile.toPath());
                BufferedReader reader = new BufferedReader(new FileReader(settingsFile));
                StringBuilder builder = new StringBuilder();
                String currentLine = reader.readLine();
                while (currentLine != null) {
                    builder.append(currentLine);
                    currentLine = reader.readLine();
                }
                reader.close();
                return Base64.encode(builder.toString());
            } else {
                return "";
            }
        } finally {
            settingsFile.delete();
            settingsFile.getParentFile().delete();
        }
    }
}
