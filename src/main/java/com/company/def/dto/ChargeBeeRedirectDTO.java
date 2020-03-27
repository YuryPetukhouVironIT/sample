package com.company.def.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargeBeeRedirectDTO {

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("redirectUrl")
    private final String redirectUrl;

    public ChargeBeeRedirectDTO (final boolean success, final String redirectUrl) {
        this.success = success;
        this.redirectUrl = redirectUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}
