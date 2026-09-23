package com.brian.rentaltracker.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStatusTest {

    @Test
    void availableCanMoveToRentedOrUnlisted() {
        assertTrue(ItemStatus.AVAILABLE.canTransitionTo(ItemStatus.RENTED));
        assertTrue(ItemStatus.AVAILABLE.canTransitionTo(ItemStatus.UNLISTED));
    }

    @Test
    void rentedCanMoveToAvailableOrUnlisted() {
        assertTrue(ItemStatus.RENTED.canTransitionTo(ItemStatus.AVAILABLE));
        assertTrue(ItemStatus.RENTED.canTransitionTo(ItemStatus.UNLISTED));
    }

    @Test
    void unlistedCanOnlyMoveToAvailable() {
        assertTrue(ItemStatus.UNLISTED.canTransitionTo(ItemStatus.AVAILABLE));
        assertFalse(ItemStatus.UNLISTED.canTransitionTo(ItemStatus.RENTED));
    }

    @Test
    void noStatusCanTransitionToItself() {
        for (ItemStatus status : ItemStatus.values()) {
            assertFalse(status.canTransitionTo(status));
        }
    }
}
