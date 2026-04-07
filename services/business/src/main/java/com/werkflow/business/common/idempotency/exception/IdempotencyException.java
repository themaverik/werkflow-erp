package com.werkflow.business.common.idempotency.exception;

public class IdempotencyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyException(String message) {
        super(message);
    }

    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
