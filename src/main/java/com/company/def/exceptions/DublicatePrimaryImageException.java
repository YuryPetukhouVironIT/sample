package com.company.def.exceptions;

public class DublicatePrimaryImageException extends RuntimeException {
    private static final long serialVersionUID = -6009604638922919798L;
    private long primaryId;

    public DublicatePrimaryImageException(long primaryId) {
        this.primaryId = primaryId;
    }

    @Override
    public String getMessage() {
        return "Primary image for this type exists";
    }

    public long getPrimaryId() {
        return primaryId;
    }
}
