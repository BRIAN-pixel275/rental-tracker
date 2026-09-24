package com.tracker.rentaltracker.service;

import com.tracker.rentaltracker.domain.Item;
import com.tracker.rentaltracker.domain.ItemStatus;
import com.tracker.rentaltracker.domain.User;
import com.tracker.rentaltracker.exception.BusinessRuleException;
import com.tracker.rentaltracker.infrastructure.DatabaseManager;
import com.tracker.rentaltracker.infrastructure.OwnerConfig;
import com.tracker.rentaltracker.repository.ItemRepository;
import com.tracker.rentaltracker.repository.RentalRepository;
import com.tracker.rentaltracker.repository.UserRepository;
import com.tracker.rentaltracker.testsupport.TestDatabaseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryServiceTest {

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
    void listingSetsItemToAvailable() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        assertEquals(ItemStatus.AVAILABLE, item.getStatus());
    }

    @Test
    void delistSetsItemToUnlisted() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        inventoryService.delist(item.getItemId());
        assertEquals(ItemStatus.UNLISTED, inventoryService.getItem(item.getItemId()).getStatus());
    }

    @Test
    void relistReturnsItemToAvailable() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        inventoryService.delist(item.getItemId());
        inventoryService.relist(item.getItemId());
        assertEquals(ItemStatus.AVAILABLE, inventoryService.getItem(item.getItemId()).getStatus());
    }

    @Test
    void cannotRelistAnItemThatIsNotUnlisted() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        assertThrows(BusinessRuleException.class, () -> inventoryService.relist(item.getItemId()));
    }

    @Test
    void cannotRelistWhileStillOutOnRental() {
        Item item = inventoryService.listItem(ownerId, "Ladder", "6-step", 5.0);
        rentalService.recordRental(item.getItemId(), "meelis", 3);
        inventoryService.delist(item.getItemId()); // delisted while it's out
        assertThrows(BusinessRuleException.class, () -> inventoryService.relist(item.getItemId()));
    }
}
