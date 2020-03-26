package com.cephx.def.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShareLinksDTO {

    @JsonProperty("readOnlyShareLink")
    private final String readOnlyShareLink;

    @JsonProperty("readWriteShareLink")
    private final String readWriteShareLink;

    public ShareLinksDTO (final String readOnlyShareLink, final String readWriteShareLink) {
        this.readOnlyShareLink = readOnlyShareLink;
        this.readWriteShareLink = readWriteShareLink;
    }

    public String getReadOnlyShareLink() {
        return readOnlyShareLink;
    }

    public String getReadWriteShareLink() {
        return readWriteShareLink;
    }
}
