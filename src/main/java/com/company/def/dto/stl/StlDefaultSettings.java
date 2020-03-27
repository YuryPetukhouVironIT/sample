package com.company.def.dto.stl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class StlDefaultSettings {

    private String bonesColor;
    private Double bonesOpacity;
    private String nervesColor;
    private Double nervesOpacity;
    private String teethColor;
    private Double teethOpacity;

    public StlDefaultSettings(){
    }

    public StlDefaultSettings(final String bonesColor, final Double bonesOpacity, final String nervesColor, final Double nervesOpacity, final String teethColor, final Double teethOpacity) {
        this.bonesColor = bonesColor;
        this.bonesOpacity = bonesOpacity;
        this.nervesColor = nervesColor;
        this.nervesOpacity = nervesOpacity;
        this.teethColor = teethColor;
        this.teethOpacity = teethOpacity;
    }

    @JsonProperty("bonesColor")
    public String getBonesColor() {
        return bonesColor;
    }

    @JsonProperty("bonesOpacity")
    public Double getBonesOpacity() {
        return bonesOpacity;
    }

    @JsonProperty("nervesColor")
    public String getNervesColor() {
        return nervesColor;
    }

    @JsonProperty("nervesOpacity")
    public Double getNervesOpacity() {
        return nervesOpacity;
    }

    @JsonProperty("teethColor")
    public String getTeethColor() {
        return teethColor;
    }

    @JsonProperty("teethOpacity")
    public Double getTeethOpacity() {
        return teethOpacity;
    }

    public StlDefaultSettings setBonesColor(String bonesColor) {
        this.bonesColor = bonesColor;
        return this;
    }

    public StlDefaultSettings setBonesOpacity(Double bonesOpacity) {
        this.bonesOpacity = bonesOpacity;
        return this;
    }

    public StlDefaultSettings setNervesColor(String nervesColor) {
        this.nervesColor = nervesColor;
        return this;
    }

    public StlDefaultSettings setNervesOpacity(Double nervesOpacity) {
        this.nervesOpacity = nervesOpacity;
        return this;
    }

    public StlDefaultSettings setTeethColor(String teethColor) {
        this.teethColor = teethColor;
        return this;
    }

    public StlDefaultSettings setTeethOpacity(Double teethOpacity) {
        this.teethOpacity = teethOpacity;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StlDefaultSettings)) {
            return false;
        }
        StlDefaultSettings that = (StlDefaultSettings) o;
        return Objects.equals(bonesColor, that.bonesColor) &&
            Objects.equals(bonesOpacity, that.bonesOpacity) &&
            Objects.equals(nervesColor, that.nervesColor) &&
            Objects.equals(nervesOpacity, that.nervesOpacity) &&
            Objects.equals(teethColor, that.teethColor) &&
            Objects.equals(teethOpacity, that.teethOpacity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bonesColor, bonesOpacity, nervesColor, nervesOpacity, teethColor, teethOpacity);
    }

    @Override
    public String toString() {
        return "StlDefaultSettings{" +
            "bonesColor='" + bonesColor + '\'' +
            ", bonesOpacity=" + bonesOpacity +
            ", nervesColor='" + nervesColor + '\'' +
            ", nervesOpacity=" + nervesOpacity +
            ", teethColor='" + teethColor + '\'' +
            ", teethOpacity=" + teethOpacity +
            '}';
    }
}
