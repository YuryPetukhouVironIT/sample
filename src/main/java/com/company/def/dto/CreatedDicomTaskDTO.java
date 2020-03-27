package com.company.def.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatedDicomTaskDTO {

    @JsonProperty("dicomTaskId")
    private final long dicomTaskId;

    @JsonProperty("patientId")
    private final long patientId;

    public CreatedDicomTaskDTO (final long patientId, final long dicomTaskId) {
        this.patientId = patientId;
        this.dicomTaskId = dicomTaskId;
    }

    public long getDicomTaskId() {
        return dicomTaskId;
    }

    public long getPatientId() {
        return patientId;
    }
}
