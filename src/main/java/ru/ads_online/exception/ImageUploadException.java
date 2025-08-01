package ru.ads_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

public class ImageUploadException extends ResponseStatusException {
    public ImageUploadException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public ImageUploadException(String message, IOException e) {
        super(HttpStatus.BAD_REQUEST, message, e);
    }

}
