package com.cephx.def.exceptions;

public class NoSuchDoctorException extends RuntimeException {
    private final long doctorNumber;

    public NoSuchDoctorException(long doctorNumber) {
        this.doctorNumber = doctorNumber;
    }

    @Override
    public String getMessage() {
        return "Doctor with number " + doctorNumber + " doesn't exist in the system.";
    }
}
