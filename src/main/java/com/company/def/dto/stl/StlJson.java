package com.company.def.dto.stl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StlJson {

    private final StlDefaultSettings defaultSettings;
    private final StlData data;

    public StlJson(final StlDefaultSettings defaultSettings, final StlData data) {
        this.defaultSettings = defaultSettings;
        this.data = data;
    }

    @JsonProperty("defaultSettings")
    public StlDefaultSettings getDefaultSettings() {
        return defaultSettings;
    }

    @JsonProperty("data")
    public StlData getData() {
        return data;
    }
}
