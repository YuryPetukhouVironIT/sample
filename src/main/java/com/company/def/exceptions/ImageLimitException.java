package com.cephx.def.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Too many images for stage")
public class ImageLimitException extends RuntimeException {
    @Override
    public String getMessage() {
        return "User uploaded maximum images";
    }
}
