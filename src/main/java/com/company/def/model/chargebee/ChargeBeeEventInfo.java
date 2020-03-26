package com.cephx.def.model.chargebee;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "cb_events")
public class ChargeBeeEventInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @Column(name = "received_dt")
    @Temporal(TemporalType.TIMESTAMP)
    @Type(type = "timestamp")
    private Date receivedDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @Column(name = "processed_dt")
    @Type(type = "timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date processedDateTime;

    @Column(name = "is_processed")
    private Boolean processed;

    @Column(name = "is_skipped")
    private Boolean skipped;

    @Column(name = "json")
    private String json;

    @Column(name = "resource_version")
    private Long resourceVersion;

    @Column(name = "occured_at")
    private Long occuredAt;

    public ChargeBeeEventInfo() {
    }

    public Integer getId() {
        return id;
    }

    public ChargeBeeEventInfo setId(Integer id) {
        this.id = id;
        return this;
    }

    public Date getReceivedDateTime() {
        return receivedDateTime;
    }

    public ChargeBeeEventInfo setReceivedDateTime(Date receivedDateTime) {
        this.receivedDateTime = receivedDateTime;
        return this;
    }

    public Date getProcessedDateTime() {
        return processedDateTime;
    }

    public ChargeBeeEventInfo setProcessedDateTime(Date processedDateTime) {
        this.processedDateTime = processedDateTime;
        return this;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public ChargeBeeEventInfo setProcessed(Boolean processed) {
        this.processed = processed;
        return this;
    }

    public String getJson() {
        return json;
    }

    public ChargeBeeEventInfo setJson(String json) {
        this.json = json;
        return this;
    }

    public Long getResourceVersion() {
        return resourceVersion;
    }

    public ChargeBeeEventInfo setResourceVersion(Long resourceVersion) {
        this.resourceVersion = resourceVersion;
        return this;
    }

    public Boolean getSkipped() {
        return skipped;
    }

    public ChargeBeeEventInfo setSkipped(Boolean skipped) {
        this.skipped = skipped;
        return this;
    }

    public Long getOccuredAt() {
        return occuredAt;
    }

    public ChargeBeeEventInfo setOccuredAt(Long occuredAt) {
        this.occuredAt = occuredAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChargeBeeEventInfo)) {
            return false;
        }
        ChargeBeeEventInfo that = (ChargeBeeEventInfo) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(receivedDateTime, that.receivedDateTime) &&
            Objects.equals(processedDateTime, that.processedDateTime) &&
            Objects.equals(processed, that.processed) &&
            Objects.equals(skipped, that.skipped) &&
            Objects.equals(json, that.json) &&
            Objects.equals(resourceVersion, that.resourceVersion) &&
            Objects.equals(occuredAt, that.occuredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, receivedDateTime, processedDateTime, processed, skipped, json, resourceVersion, occuredAt);
    }

    @Override
    public String toString() {
        return "ChargeBeeEventInfo{" +
            "id=" + id +
            ", receivedDateTime=" + receivedDateTime +
            ", processedDateTime=" + processedDateTime +
            ", processed=" + processed +
            ", skipped=" + skipped +
            ", json='" + json + '\'' +
            ", resourceVersion=" + resourceVersion +
            ", occuredAt=" + occuredAt +
            '}';
    }
}
