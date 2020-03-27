package com.company.def.model.airways;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AirwaysSlice {
    private double area;
    private double apDiameter;
    private double rlDiameter;
    private double slicePosition;

    public AirwaysSlice() {
    }

    @JsonProperty("area")
    public double getArea() {
        return area;
    }

    public void setArea(final double area) {
        this.area = area;
    }

    @JsonProperty("ap_diameter")
    public double getApDiameter() {
        return apDiameter;
    }

    public void setApDiameter(final double apDiameter) {
        this.apDiameter = apDiameter;
    }

    @JsonProperty("rl_diameter")
    public double getRlDiameter() {
        return rlDiameter;
    }

    public void setRlDiameter(final double rlDiameter) {
        this.rlDiameter = rlDiameter;
    }

    @JsonProperty("slice_position")
    public double getSlicePosition() {
        return slicePosition;
    }

    public void setSlicePosition(final double slicePosition) {
        this.slicePosition = slicePosition;
    }
}
