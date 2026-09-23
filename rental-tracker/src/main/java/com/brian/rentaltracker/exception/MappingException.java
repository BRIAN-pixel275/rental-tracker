package com.brian.rentaltracker.exception;

/** A row could not be mapped into its domain object (wrong type, unexpected NULL). */
public class MappingException extends DatabaseException {
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
