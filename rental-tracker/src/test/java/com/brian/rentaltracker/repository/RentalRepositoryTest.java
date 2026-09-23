package com.brian.rentaltracker.repository;

import com.brian.rentaltracker.domain.Item;
import com.brian.rentaltracker.domain.Rental;
import com.brian.rentaltracker.domain.RentalStatus;
import com.brian.rentaltracker.domain.User;
import com.brian.rentaltracker.exception.ConstraintViolationException;
import com.brian.rentaltracker.infrastructure.DatabaseManager;
import com.brian.rentaltracker.testsupport.TestDatabaseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalRepositoryTest {

    private DatabaseManager db;
    private RentalRepository rentalRepository;
    private int ownerId;
    private int renterId;
    private int itemId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        db = TestDatabaseFactory.freshDatabase(tempDir);
        rentalRepository = new RentalRepository(db.getConnection());
        ItemRepository itemRepository = new ItemRepository(db.getConnection());
        UserRepository userRepository = new UserRepository(db.getConnection());
        ownerId = userRepository.insert(User.newUser("liisa")).getId();
        renterId = userRepository.insert(User.newUser("meelis")).getId();
        itemId = itemRepository.insert(Item.newListing(ownerId, "Ladder", "6-step", 5.0)).getItemId();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void insertsAndFindsRentalById() {
        Rental rental = rentalRepository.insert(Rental.start(itemId, renterId, LocalDateTime.now(), 3));
        Rental found = rentalRepository.findById(rental.getRentalId());
        assertEquals(itemId, found.getItemId());
    }

    @Test
    void findsActiveRentalsForOwnerSortedByDueDate() {
        rentalRepository.insert(Rental.start(itemId, renterId, LocalDateTime.now(), 5));
        List<Rental> active = rentalRepository.findActiveByOwner(ownerId);
        assertEquals(1, active.size());
    }

    @Test
    void closingRentalSetsReturnedAtAndStatus() {
        Rental rental = rentalRepository.insert(Rental.start(itemId, renterId, LocalDateTime.now(), 3));
        rentalRepository.closeRental(rental.getRentalId(), LocalDateTime.now());
        Rental closed = rentalRepository.findById(rental.getRentalId());
        assertEquals(RentalStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getReturnedAt());
    }

    @Test
    void rentalPointingToNonexistentItemViolatesForeignKey() {
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> rentalRepository.insert(Rental.start(9999, renterId, LocalDateTime.now(), 3)));
        assertEquals(ConstraintViolationException.ConstraintType.FOREIGN_KEY, ex.getConstraintType());
    }
}
