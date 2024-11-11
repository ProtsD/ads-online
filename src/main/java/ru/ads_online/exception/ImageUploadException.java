package ru.ads_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

public class ImageUploadException extends ResponseStatusException {
    public ImageUploadException(String message) {
        super(HttpStatus.NOT_ACCEPTABLE, message);
    }

    public ImageUploadException(String message, IOException e) {
        super(HttpStatus.NOT_ACCEPTABLE, message, e);
    }

}
