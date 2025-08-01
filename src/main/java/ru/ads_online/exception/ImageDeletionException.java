package ru.ads_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ImageDeletionException extends ResponseStatusException {
    public ImageDeletionException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
