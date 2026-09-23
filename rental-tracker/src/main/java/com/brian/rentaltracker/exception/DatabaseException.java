package com.brian.rentaltracker.exception;

/** Base type for every failure that originates in the repository layer. */
public abstract class DatabaseException extends RuntimeException {
    protected DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    protected DatabaseException(String message) {
        super(message);
    }
}
