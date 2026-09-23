package com.brian.rentaltracker.exception;

/** The database file could not be opened (missing, bad path, or bad permissions). */
public class DatabaseConnectionException extends DatabaseException {
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
    public DatabaseConnectionException(String message) {
        super(message);
    }
}
