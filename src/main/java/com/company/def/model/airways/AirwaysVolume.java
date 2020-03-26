package com.cephx.def.model.airways;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AirwaysVolume {

    private double totalVolume;
    private MinimalAreaSlice minimalAreaSlice;
    private MaximalAreaSlice maximalAreaSlice;
    private MinimalApDiameterSlice minimalApDiameterSlice;
    private MinimalRlDiameterSlice minimalRlDiameterSlice;

    @JsonProperty("total_volume")
    public double getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(final double totalVolume) {
        this.totalVolume = totalVolume;
    }

    @JsonProperty("minimal_area_slice")
    public MinimalAreaSlice getMinimalAreaSlice() {
        return minimalAreaSlice;
    }

    public void setMinimalAreaSlice(final MinimalAreaSlice minimalAreaSlice) {
        this.minimalAreaSlice = minimalAreaSlice;
    }

    @JsonProperty("maximal_area_slice")
    public MaximalAreaSlice getMaximalAreaSlice() {
        return maximalAreaSlice;
    }

    public void setMaximalAreaSlice(final MaximalAreaSlice maximalAreaSlice) {
        this.maximalAreaSlice = maximalAreaSlice;
    }

    @JsonProperty("minimal_ap_diam_slice")
    public MinimalApDiameterSlice getMinimalApDiameterSlice() {
        return minimalApDiameterSlice;
    }

    public void setMinimalApDiameterSlice(final MinimalApDiameterSlice minimalApDiameterSlice) {
        this.minimalApDiameterSlice = minimalApDiameterSlice;
    }

    @JsonProperty("minimal_rl_diam_slice")
    public MinimalRlDiameterSlice getMinimalRlDiameterSlice() {
        return minimalRlDiameterSlice;
    }

    public void setMinimalRlDiameterSlice(MinimalRlDiameterSlice minimalRlDiameterSlice) {
        this.minimalRlDiameterSlice = minimalRlDiameterSlice;
    }
}
