package com.example.ledgerservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TransferAlreadyExistsException extends RuntimeException {
    public TransferAlreadyExistsException(String message) {
        super(message);
    }
}
