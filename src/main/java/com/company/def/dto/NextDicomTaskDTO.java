package com.company.def.dto;

import com.company.def.BasicDoc;
import com.company.def.BasicPatientInfoData;
import com.company.def.enums.DicomType;
import com.company.def.model.DicomTask;

public class NextDicomTaskDTO {

    private final long transactionId;
    private final String s3Key;
    private final DicomType dicomType;
    private final long patientId;
    private final boolean completed;
    private final boolean isProgress;
    private final Long timestamp;
    private final boolean taskStuck;
    private final long doctorId;
    private final String patientName;
    private final String firstName;
    private final String lastName;


    public NextDicomTaskDTO(final DicomTask dicomTask, final BasicDoc doctor, final BasicPatientInfoData patientInfoData) {
        this.transactionId = dicomTask.getTransactionId();
        this.s3Key = dicomTask.getS3Key();
        this.dicomType = dicomTask.getDicomType();
        this.patientId = dicomTask.getPatientId();
        this.completed = dicomTask.isCompleted();
        this.isProgress = dicomTask.isProgress();
        this.timestamp = dicomTask.getTimestamp();
        this.taskStuck = dicomTask.isTaskStuck();
        this.doctorId = doctor.docnum;
        this.patientName = patientInfoData.getPatientFullName();
        this.firstName = patientInfoData.getPatientFirstName();
        this.lastName = patientInfoData.getPatientLastName();
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public DicomType getDicomType() {
        return dicomType;
    }

    public long getPatientId() {
        return patientId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isProgress() {
        return isProgress;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public boolean isTaskStuck() {
        return taskStuck;
    }

    public long getDoctorId() {
        return doctorId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }
}
