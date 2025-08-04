package com.example.transferservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class LedgerServiceException extends RuntimeException {
    public LedgerServiceException(String message) {
        super(message);
    }
}
