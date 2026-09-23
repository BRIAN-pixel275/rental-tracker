package com.brian.rentaltracker.domain;

public enum RentalStatus {
    ACTIVE, CLOSED;

    public boolean canTransitionTo(RentalStatus target) {
        return this == ACTIVE && target == CLOSED;
    }

    public static RentalStatus fromDb(String value) {
        return RentalStatus.valueOf(value.toUpperCase());
    }

    public String toDb() {
        return name().toLowerCase();
    }
}
