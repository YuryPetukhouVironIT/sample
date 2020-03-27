package com.company.def.service.db;

import com.company.def.model.IgnoredScaleSnapshot;
import com.company.def.model.ManualScaleCalibrationSnapshot;
import com.company.def.model.Screenshot;
import com.company.def.repository.IgnoredScaleSnapshotRepository;
import com.company.def.repository.ManualScaleCalibrationRepository;
import com.company.def.repository.ScreenshotRepository;
import com.company.def.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

@Service
public class EstimatedScaleService {
    @Autowired
    private IgnoredScaleSnapshotRepository ignoredScaleSnapshotRepository;
    @Autowired
    private ManualScaleCalibrationRepository manualScaleCalibrationRepository;
    @Autowired
    private ScreenshotRepository screenshotRepository;
    @Autowired
    private S3Service s3Service;

    @Transactional
    public void saveIgnoredScaleSnapshot(final IgnoredScaleSnapshot snapshot, final File screenshotFile) {
        uploadAndSaveScreenshot(snapshot.getScreenshot(), screenshotFile);
        ignoredScaleSnapshotRepository.save(snapshot);
    }

    @Transactional
    public void saveManualCalibrationSnapshot(final ManualScaleCalibrationSnapshot snapshot, final File screenshotFile) {
        uploadAndSaveScreenshot(snapshot.getScreenshot(), screenshotFile);
        manualScaleCalibrationRepository.save(snapshot);
    }

    private void uploadAndSaveScreenshot(final Screenshot screenshot, final File screenshotFile) {
        screenshotRepository.save(screenshot);
        String screenshotKey = s3Service.uploadScreenshotToS3(screenshot.getId(), screenshotFile);
        screenshot.setS3Key(screenshotKey);
        screenshotRepository.saveAndFlush(screenshot);
    }
}
