package com.company.def.dto.stl;

import com.company.def.funcclass;
import com.company.def.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StlJsonCreator {

    private static final Logger logger = LogManager.getLogger(StlJsonCreator.class);
    private static final String DEFAULT_SETTINGS_FILE_NAME = "stlViewerDefaultSettings.json"
    private static final String STL_VIEWER_PREFERENCES_FILE_NAME = "stlViewerPreferences.json"
    private final Long patientId;
    private final String patientFirstName;
    private final String patientLastName;
    private final String stl1S3Path;
    private final List<String> stlFileNames;
    private final String mobileZipFileUrl;
    private final String desktopZipFileUrl;
    private final boolean isCompany;
    private StlPreferences stlPreferences;

    public StlJsonCreator(final Long patientId, final String patientFirstName, final String patientLastName, final boolean isCompany, final String stl1S3Path, final S3Service s3Service, final String mobileZipFileUrl, final String desktopZipFileUrl, final String zipFileName) {
        this.patientId = patientId;
        this.patientFirstName = patientFirstName;
        this.patientLastName = patientLastName;
        this.stl1S3Path = stl1S3Path;
        this.stlFileNames = initStlFileNames(stl1S3Path, s3Service, zipFileName);
        this.mobileZipFileUrl = mobileZipFileUrl;
        this.desktopZipFileUrl = desktopZipFileUrl;
        this.stlPreferences = initDefaultStlPreferences();
        this.isCompany = isCompany;
    }

    private List<String> initStlFileNames(final String stl1S3Path, final S3Service s3Service, final String zipFileName) {
        final String zipFileKey = stl1S3Path + "/" + zipFileName;
        final List<String> fileNames = new ArrayList<>();
        final String tempDir = funcclass.tempPatientImagesPath;
        final File directory = new File(tempDir + "/dicomDirViewer" + patientId);
        directory.mkdirs();
        final File destinationFile = new File(directory.getAbsolutePath() + "/" + zipFileName);
        final File zip = s3Service.downloadSingleFileFromS3(zipFileKey, destinationFile.toPath());
        try (ZipFile zipFile = new ZipFile(zip.getAbsolutePath())) {
            final Enumeration zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                fileNames.add(((ZipEntry) zipEntries.nextElement()).getName());
            }
            zip.delete();
        } catch (IOException e) {
            logger.error(e);
        }
        return fileNames;
    }

    public StlJson createStlJson() throws Exception {
        final StlDefaultSettings defaultSettings = getDefaultSettings();
        final List<StlFile> bones = getDefaultBonesFiles();
        final List<StlFile> nerves = getDefaultNervesFiles();
        final List<StlDentation> dentations = getDefaultDentations();
        final String shareLink = getDefaultShareLink();
        final StlData stlData = new StlData(patientId, patientFirstName, patientLastName, isCompany, shareLink, mobileZipFileUrl, desktopZipFileUrl, bones, nerves, dentations);
        return new StlJson(defaultSettings, stlData);
    }

    private String getDefaultShareLink() throws UnsupportedEncodingException {
        return funcclass.resourcesUrl + "assets/plugins/stl_viewer_upd/STLViewer.html?json_location=" + URLEncoder.encode(funcclass.s3UrlDirect + stl1S3Path + "/zipinput.json", StandardCharsets.UTF_8.toString());
    }

    private List<StlDentation> getDefaultDentations() {
        final List<StlDentation> dentations = new ArrayList<>();
        dentations.add(getDefaultMaxillaDentation());
        dentations.add(getDefaultMandibleDentation());
        dentations.add(getDefaultOthersDentation());
        return dentations;
    }

    private StlDentation getDefaultOthersDentation() {
        return new StlDentation("Other", getDeafaultOtherTeethGroup(), null);
    }

    private List<StlToothGroup> getDeafaultOtherTeethGroup() {
        final List<StlToothGroup> teethGroups = new ArrayList<>();
        final List<StlFile> otherTeethFiles = getOtherTeethFiles();
        teethGroups.add(new StlToothGroup("other", otherTeethFiles));
        return teethGroups;
    }

    private List<StlFile> getOtherTeethFiles() {
        final List<StlFile> files = new ArrayList<>();
        int[] teethFilesInGroups = stlPreferences.getTeethNumbersInGroups();
        for (String fileName : stlFileNames) {
            if (fileName.matches("tooth_\\d+\\.stl")) {
                int toothNumber = Integer.parseInt(fileName.split("_")[1].split("\\.")[0]);
                if (!ArrayUtils.contains(teethFilesInGroups, toothNumber)) {
                    files.add(new StlFile(String.valueOf(toothNumber), fileName, stlPreferences.getOtherTeethColor()));
                }
            }
        }
        return files;
    }

    private StlDentation getDefaultMaxillaDentation() {
        return new StlDentation("Maxilla", getDefaultMaxillaToothGroups(), null);
    }

    private List<StlToothGroup> getDefaultMaxillaToothGroups() {
        final List<StlToothGroup> maxillaToothGroups = new ArrayList<>();
        addToothGroupIfNotEmpty(maxillaToothGroups, 11, 19);
        addToothGroupIfNotEmpty(maxillaToothGroups, 21, 29);
        return maxillaToothGroups;
    }


    private StlDentation getDefaultMandibleDentation() {
        return new StlDentation("Mandible", getDefaultMandibleToothGroups(), null);
    }

    private List<StlToothGroup> getDefaultMandibleToothGroups() {
        final List<StlToothGroup> mandibleToothGroups = new ArrayList<>();
        addToothGroupIfNotEmpty(mandibleToothGroups, 31, 39);
        addToothGroupIfNotEmpty(mandibleToothGroups, 41, 49);
        return mandibleToothGroups;
    }

    private List<StlFile> getDefaultBonesFiles() {
        final List<StlFile> bonesFiles = new ArrayList<>();
        addStlFileIfPresent(bonesFiles, "Maxilla", "maxilla.stl");
        addStlFileIfPresent(bonesFiles, "Mandibula", "mandibula.stl");
        return bonesFiles;
    }

    private List<StlFile> getDefaultNervesFiles() {
        final List<StlFile> nervesFiles = new ArrayList<>();
        addStlFileIfPresent(nervesFiles, "Merged", "merged_canals.stl");
        return nervesFiles;
    }

    private void addStlFileIfPresent(final List<StlFile> stlFiles, final String name, final String fileName) {
        if (stlFileNames.contains(fileName)) {
            stlFiles.add(new StlFile(name, fileName));
        }
    }

    private void addStlFileIfPresent(final List<StlFile> stlFiles, final String name, final String fileName, final String color) {
        if (stlFileNames.contains(fileName)) {
            stlFiles.add(new StlFile(name, fileName, color));
        }
    }

    private void addToothGroupIfNotEmpty(List<StlToothGroup> toothGroups, int startTooth, int finishTooth) {
        final List<StlFile> toothFiles = getDefaultToothFilesIfPresent(startTooth, finishTooth);
        if (!toothFiles.isEmpty()) {
            toothGroups.add(new StlToothGroup(String.valueOf(startTooth) + "-" + String.valueOf(finishTooth), toothFiles));
        }
    }

    private List<StlFile> getDefaultToothFilesIfPresent(int startTooth, int finishTooth) {
        final List<StlFile> toothFiles = new ArrayList<>();
        for (int i = startTooth; i <= finishTooth; i++) {
            if (stlFileNames.contains("tooth_" + i + ".stl")) {
                toothFiles.add(new StlFile(String.valueOf(i), "tooth_" + i + ".stl"));
            }
        }
        return toothFiles;
    }

    private StlDefaultSettings getDefaultSettings() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final StlDefaultSettings defaultSettings = objectMapper.readValue(new File(funcclass.innerLogDir + DEFAULT_SETTINGS_FILE_NAME), StlDefaultSettings.class);
        return defaultSettings;
    }

    private StlPreferences initDefaultStlPreferences() {
        try {
            return new ObjectMapper().readValue(new File(funcclass.innerLogDir + STL_VIEWER_PREFERENCES_FILE_NAME, StlPreferences.class);
        } catch (IOException e) {
            logger.error(e);
            return new StlPreferences();
        }

    }

}
