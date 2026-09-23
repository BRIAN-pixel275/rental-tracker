package com.brian.rentaltracker.exception;

/**
 * Thrown by the service layer when an action violates a business rule rather
 * than a database constraint — e.g. renting an item that's already rented,
 * or relisting an item that's still out.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
