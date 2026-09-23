package com.brian.rentaltracker.exception;

/** A write broke a NOT NULL, UNIQUE, CHECK, or foreign key rule. */
public class ConstraintViolationException extends DatabaseException {

    public enum ConstraintType { UNIQUE, NOT_NULL, CHECK, FOREIGN_KEY, UNKNOWN }

    private final ConstraintType constraintType;

    public ConstraintViolationException(ConstraintType constraintType, String message, Throwable cause) {
        super(message, cause);
        this.constraintType = constraintType;
    }

    public ConstraintType getConstraintType() {
        return constraintType;
    }
}
