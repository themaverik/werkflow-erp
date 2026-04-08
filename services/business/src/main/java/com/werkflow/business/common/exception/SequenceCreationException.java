package com.werkflow.business.common.exception;

/**
 * Thrown when a database sequence cannot be created or accessed.
 */
public class SequenceCreationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SequenceCreationException(String message) {
        super(message);
    }

    public SequenceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
