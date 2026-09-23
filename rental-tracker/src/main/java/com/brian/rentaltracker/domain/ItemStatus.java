package com.brian.rentaltracker.domain;

/**
 * The item's lifecycle, enforced here rather than only in the database CHECK
 * constraint. Every status change in the service layer must go through
 * canTransitionTo() first.
 */
public enum ItemStatus {
    AVAILABLE, RENTED, UNLISTED;

    public boolean canTransitionTo(ItemStatus target) {
        return switch (this) {
            case AVAILABLE -> target == RENTED || target == UNLISTED;
            case RENTED -> target == AVAILABLE || target == UNLISTED;
            case UNLISTED -> target == AVAILABLE;
        };
    }

    public static ItemStatus fromDb(String value) {
        return ItemStatus.valueOf(value.toUpperCase());
    }

    public String toDb() {
        return name().toLowerCase();
    }
}
