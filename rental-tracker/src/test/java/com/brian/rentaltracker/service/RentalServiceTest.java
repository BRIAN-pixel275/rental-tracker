package com.brian.rentaltracker.service;

import com.brian.rentaltracker.domain.Item;
import com.brian.rentaltracker.domain.ItemStatus;
import com.brian.rentaltracker.domain.Rental;
import com.brian.rentaltracker.domain.User;
import com.brian.rentaltracker.exception.BusinessRuleException;
import com.brian.rentaltracker.infrastructure.DatabaseManager;
import com.brian.rentaltracker.infrastructure.OwnerConfig;
import com.brian.rentaltracker.repository.ItemRepository;
import com.brian.rentaltracker.repository.RentalRepository;
import com.brian.rentaltracker.repository.UserRepository;
import com.brian.rentaltracker.testsupport.TestDatabaseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RentalServiceTest {

    private DatabaseManager db;
    private InventoryService inventoryService;
    private RentalService rentalService;
    private int ownerId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        db = TestDatabaseFactory.freshDatabase(tempDir);
        UserRepository userRepository = new UserRepository(db.getConnection());
        ItemRepository itemRepository = new ItemRepository(db.getConnection());
        RentalRepository rentalRepository = new RentalRepository(db.getConnection());
        UserService userService = new UserService(userRepository, new OwnerConfig(tempDir.resolve("owner.cfg")));

        inventoryService = new InventoryService(itemRepository, rentalRepository);
        rentalService = new RentalService(itemRepository, rentalRepository, userService);
        ownerId = userRepository.insert(User.newUser("liisa")).getId();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void recordingRentalSetsItemToRented() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        rentalService.recordRental(item.getItemId(), "meelis", 3);
        assertEquals(ItemStatus.RENTED, inventoryService.getItem(item.getItemId()).getStatus());
    }

    @Test
    void cannotRentAnAlreadyRentedItem() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        rentalService.recordRental(item.getItemId(), "meelis", 3);
        assertThrows(BusinessRuleException.class,
                () -> rentalService.recordRental(item.getItemId(), "siim", 2));
    }

    @Test
    void cannotRentADelistedItem() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        inventoryService.delist(item.getItemId());
        assertThrows(BusinessRuleException.class,
                () -> rentalService.recordRental(item.getItemId(), "meelis", 3));
    }

    @Test
    void rentingToExistingUsernameReusesTheRecord() {
        Item item1 = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        Item item2 = inventoryService.listItem(ownerId, "Drill", "Cordless", 4.0);
        Rental first = rentalService.recordRental(item1.getItemId(), "meelis", 3);
        Rental second = rentalService.recordRental(item2.getItemId(), "meelis", 2);
        assertEquals(first.getRenterId(), second.getRenterId());
    }

    @Test
    void endTimeEqualsStartPlusDuration() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        Rental rental = rentalService.recordRental(item.getItemId(), "meelis", 4);
        assertEquals(Duration.ofDays(4), Duration.between(rental.getStartTime(), rental.getEndTime()));
    }

    @Test
    void confirmingReturnSetsItemBackToAvailable() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        Rental rental = rentalService.recordRental(item.getItemId(), "meelis", 3);
        rentalService.confirmReturn(rental.getRentalId());
        assertEquals(ItemStatus.AVAILABLE, inventoryService.getItem(item.getItemId()).getStatus());
    }

    @Test
    void confirmingReturnKeepsItemUnlistedIfDelistedWhileOut() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        Rental rental = rentalService.recordRental(item.getItemId(), "meelis", 3);
        inventoryService.delist(item.getItemId());
        rentalService.confirmReturn(rental.getRentalId());
        assertEquals(ItemStatus.UNLISTED, inventoryService.getItem(item.getItemId()).getStatus());
    }

    @Test
    void firstLaunchCreatesUserAndSecondRunLoadsSameAccount(@TempDir Path tempDir) {
        DatabaseManager bootstrapDb = TestDatabaseFactory.freshDatabase(tempDir);
        try {
            UserRepository userRepository = new UserRepository(bootstrapDb.getConnection());
            OwnerConfig config = new OwnerConfig(tempDir.resolve("owner-bootstrap.cfg"));
            UserService bootstrapService = new UserService(userRepository, config);

            User first = bootstrapService.loadOrCreateOwner(() -> "brand-new-user");
            User second = bootstrapService.loadOrCreateOwner(() -> {
                throw new AssertionError("Should not prompt again — account already exists.");
            });
            assertEquals(first.getId(), second.getId());
        } finally {
            bootstrapDb.close();
        }
    }
}
