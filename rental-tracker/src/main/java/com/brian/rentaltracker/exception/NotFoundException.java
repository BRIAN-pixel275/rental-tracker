package com.brian.rentaltracker.exception;

/** A query expected a row and found none. */
public class NotFoundException extends DatabaseException {
    public NotFoundException(String message) {
        super(message);
    }
}
