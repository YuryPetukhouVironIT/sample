package com.company.def.exceptions;

public class NoDoctorInSessionException extends RuntimeException {
    @Override
    public String getMessage() {
        return "No doctor in session. Session is expired.";
    }
}
