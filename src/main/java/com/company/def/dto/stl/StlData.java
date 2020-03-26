package com.cephx.def.dto.stl;

import com.cephx.def.funcclass;
import com.cephx.def.service.db.PatientService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class StlData {

    private final Long patientId;
    private final String patientFirstName;
    private final String patientLastName;
    private final boolean isCompany;
    private final String shareLink;
    private final String mobileZip;
    private final String desktopZip;
    private final List<StlFile> bones;
    private final List<StlFile> nerves;
    private final List<StlDentation> dentations;
    private final String readOnlyGuid;
    private final String readWriteGuid;

    public StlData(final Long patientId, final String patientFirstName, final String patientLastName, final boolean isCompany, final String shareLink, final String mobileZip, final String desktopZip,final List<StlFile> bones, final List<StlFile> nerves, final List<StlDentation> dentations) {
        this.patientId = patientId;
        this.patientFirstName = patientFirstName;
        this.patientLastName = patientLastName;
        this.isCompany = isCompany;
        this.shareLink = shareLink;
        this.mobileZip = mobileZip;
        this.desktopZip = desktopZip;
        this.bones = bones;
        this.nerves = nerves;
        this.dentations = dentations;
        this.readOnlyGuid = shareLink+"&guid="+PatientService.stlReadOnlyGuid(patientId);
        this.readWriteGuid= shareLink+"&guid="+PatientService.stlReadWriteGuid(patientId);
    }

    @JsonProperty("patientId")
    public Long getPatientId() {
        return patientId;
    }

    @JsonProperty("patientFirstName")
    public String getPatientFirstName() {
        return patientFirstName;
    }

    @JsonProperty("patientLastName")
    public String getPatientLastName() {
        return patientLastName;
    }

    @JsonProperty("mobileZip")
    public String getMobileZip() {
        return mobileZip;
    }

    @JsonProperty("desktopZip")
    public String getDesktopZip() {
        return desktopZip;
    }

    @JsonProperty("shareLink")
    public String getShareLink() {
        return shareLink;
    }

    @JsonProperty("bones")
    public List<StlFile> getBones() {
        return bones;
    }

    @JsonProperty("nerves")
    public List<StlFile> getNerves() {
        return nerves;
    }

    @JsonProperty("iscompany")
    public boolean isCompany() {
        return isCompany;
    }

    @JsonProperty("dentations")
    public List<StlDentation> getDentations() {
        return dentations;
    }

    @JsonProperty("readonly")
    public String getReadOnlyGuid() {
        return readOnlyGuid;
    }

    @JsonProperty("readwrite")
    public String getReadWriteGuid() {
        return readWriteGuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlData)) {
            return false;
        }
        StlData stlData = (StlData) o;
        return isCompany == stlData.isCompany &&
            Objects.equals(patientId, stlData.patientId) &&
            Objects.equals(patientFirstName, stlData.patientFirstName) &&
            Objects.equals(patientLastName, stlData.patientLastName) &&
            Objects.equals(shareLink, stlData.shareLink) &&
            Objects.equals(mobileZip, stlData.mobileZip) &&
            Objects.equals(desktopZip, stlData.desktopZip) &&
            Objects.equals(bones, stlData.bones) &&
            Objects.equals(nerves, stlData.nerves) &&
            Objects.equals(dentations, stlData.dentations) &&
            Objects.equals(readOnlyGuid, stlData.readOnlyGuid) &&
            Objects.equals(readWriteGuid, stlData.readWriteGuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId, patientFirstName, patientLastName, isCompany, shareLink, mobileZip, desktopZip, bones, nerves, dentations, readOnlyGuid, readWriteGuid);
    }

    @Override
    public String toString() {
        return "StlData{" +
            "patientId=" + patientId +
            ", patientFirstName='" + patientFirstName + '\'' +
            ", patientLastName='" + patientLastName + '\'' +
            ", isCompany=" + isCompany +
            ", shareLink='" + shareLink + '\'' +
            ", mobileZip='" + mobileZip + '\'' +
            ", desktopZip='" + desktopZip + '\'' +
            ", bones=" + bones +
            ", nerves=" + nerves +
            ", dentations=" + dentations +
            ", readOnlyGuid='" + readOnlyGuid + '\'' +
            ", readWriteGuid='" + readWriteGuid + '\'' +
            '}';
    }
}
