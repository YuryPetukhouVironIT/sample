package com.company.def.exceptions;

import javax.servlet.http.HttpSession;

public class NoUploaderInSessionException extends RuntimeException {

    private final HttpSession session;

    public NoUploaderInSessionException(final HttpSession session) {
        this.session = session;
    }

    @Override
    public String getMessage() {
        return "No FileUploader object in session with id "+session.getId();
    }
}
