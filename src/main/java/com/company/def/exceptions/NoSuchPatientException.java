package com.cephx.def.exceptions;

public class NoSuchPatientException extends RuntimeException {
    private final long patientNumber;

    public NoSuchPatientException(long patientNumber) {
        this.patientNumber = patientNumber;
    }

    @Override
    public String getMessage() {
        return "Patient with number " + patientNumber + " doesn't exist in the system.";
    }
}
