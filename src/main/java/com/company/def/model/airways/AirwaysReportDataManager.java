package com.company.def.model.airways;

import com.company.def.funcclass;
import com.company.def.service.S3Service;
import com.company.def.service.db.PatientService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class AirwaysReportDataManager {

    public final static String VOLUME_JSON_FILE_NAME = "airways_volume_analysis.json";
    private final static String[] PAGE_ONE_IMAGE_FILE_NAMES = {"saggital.png", "coronal.png"};
    private final static String[] PAGE_TWO_IMAGE_FILE_NAMES = {"slice_min_area.png","slice_min_ap.png","slice_bottom.png", "slice_top.png", "slice_min_rl.png"};

    private final Path directoryPath;
    private final String stlPath;
    private final S3Service s3Service;

    public AirwaysReportDataManager(final long patientId, final S3Service s3Service) {
        this.s3Service = s3Service;
        this.stlPath = PatientService.getStlPath(patientId);
        File directory = new File(funcclass.tempPatientImagesDicomPath + "/dicomDir" + patientId);
        if(!directory.exists()) {
            directory.mkdirs();
        }
        directoryPath = directory.toPath();
    }

    public String [][] reportResultsToDisplay() throws IOException {
        final String [][] results = new String[4][2];
        final AirwaysVolume airwaysVolume = airwaysVolumeReport();
        if (airwaysVolume==null) {
            return results;
        }
        final NumberFormat df = DecimalFormat.getInstance();
        df.setMinimumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        results[0] = new String[]{"Total airway volume", "Minimal area slice"};
        results[1] = new String[]{df.format(airwaysVolume.getTotalVolume()/1000.0) + " cc", df.format(airwaysVolume.getMinimalAreaSlice().getArea()) + " mm"};
        results[2] = new String[]{"Minimal AP diameter", "Minimal RL diameter"};
        results[3] = new String[]{df.format(airwaysVolume.getMinimalApDiameterSlice().getApDiameter()) + " mm", df.format(airwaysVolume.getMinimalRlDiameterSlice().getRlDiameter()) + " mm"};
        return results;
    }

    private AirwaysVolume airwaysVolumeReport() throws IOException {
        final Path tempPath = Paths.get(directoryPath.toFile().getAbsolutePath()+"_tmp");
        final File jsonFile = s3Service.downloadStlFromS3(stlPath+"/"+VOLUME_JSON_FILE_NAME, tempPath);
        if (!jsonFile.exists()) {
            return null;
        }
        return new ObjectMapper().readValue(jsonFile, AirwaysVolume.class);
    }


    public List<File> pageOneFiles () throws IOException {
        return pageFiles(PAGE_ONE_IMAGE_FILE_NAMES);
    }

    public List<File> pageTwoFiles () throws IOException {
        return pageFiles(PAGE_TWO_IMAGE_FILE_NAMES);
    }

    public void cleanFiles() {
        for (File file: directoryPath.toFile().listFiles()) {
            file.delete();
        }
        directoryPath.toFile().delete();
    }

    private List<File> pageFiles (final String[] fileNames) {
        final List<File> files = new ArrayList<>();
        for (String fileName : fileNames) {
            final Path tempPath = Paths.get(directoryPath.toFile().getAbsolutePath()+"_tmp");
            final File file = s3Service.downloadSingleFileFromS3(stlPath+"/"+fileName, tempPath);
            if (file.exists()) {
                final Path filePath = Paths.get(directoryPath.toFile().getAbsolutePath(),"files",fileName);
                File newFile = filePath.toFile();
                if (!newFile.exists()) {
                    newFile.getParentFile().mkdirs();
                }
                file.renameTo(newFile);
                files.add(newFile);
            }
        }
        return files;
    }
}
