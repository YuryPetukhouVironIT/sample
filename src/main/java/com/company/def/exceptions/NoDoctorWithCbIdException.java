package com.company.def.exceptions;

public class NoDoctorWithCbIdException extends RuntimeException {

    private final String customerId;

    public NoDoctorWithCbIdException(final String customerId) {
        this.customerId = customerId;
    }

    @Override
    public String getMessage() {
        return "Doctor with CB ID " + customerId + " doesn't exist in the system.";
    }
}
