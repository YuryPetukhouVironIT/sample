package com.cephx.def.exceptions;

public class WebApiException extends RuntimeException {

    public WebApiException() {
    }


    public WebApiException(final String message) {
        super(message);
    }


}
