package com.brian.rentaltracker.repository;

import com.brian.rentaltracker.domain.Item;
import com.brian.rentaltracker.domain.ItemStatus;
import com.brian.rentaltracker.domain.User;
import com.brian.rentaltracker.exception.ConstraintViolationException;
import com.brian.rentaltracker.exception.NotFoundException;
import com.brian.rentaltracker.exception.SqlExceptionTranslator;
import com.brian.rentaltracker.infrastructure.DatabaseManager;
import com.brian.rentaltracker.testsupport.TestDatabaseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemRepositoryTest {

    private DatabaseManager db;
    private ItemRepository itemRepository;
    private int ownerId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        db = TestDatabaseFactory.freshDatabase(tempDir);
        itemRepository = new ItemRepository(db.getConnection());
        UserRepository userRepository = new UserRepository(db.getConnection());
        ownerId = userRepository.insert(User.newUser("liisa")).getId();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void insertsAndFindsItemById() {
        Item inserted = itemRepository.insert(Item.newListing(ownerId, "Ladder", "6-step", 5.0));
        Item found = itemRepository.findById(inserted.getItemId());
        assertEquals("Ladder", found.getItemName());
        assertEquals(ItemStatus.AVAILABLE, found.getStatus());
    }

    @Test
    void findsItemsByOwner() {
        itemRepository.insert(Item.newListing(ownerId, "Ladder", "6-step", 5.0));
        itemRepository.insert(Item.newListing(ownerId, "Drill", "Cordless", 4.0));
        List<Item> items = itemRepository.findByOwner(ownerId);
        assertEquals(2, items.size());
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        assertThrows(NotFoundException.class, () -> itemRepository.findById(999));
    }

    @Test
    void updatesStatus() {
        Item inserted = itemRepository.insert(Item.newListing(ownerId, "Ladder", "6-step", 5.0));
        itemRepository.updateStatus(inserted.getItemId(), ItemStatus.RENTED);
        assertEquals(ItemStatus.RENTED, itemRepository.findById(inserted.getItemId()).getStatus());
    }

    @Test
    void missingRequiredFieldViolatesNotNullConstraint() {
        Connection connection = db.getConnection();
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO listed_items (owner_id, item_name, cost_per_day, status) VALUES (?, NULL, ?, ?)")) {
                stmt.setInt(1, ownerId);
                stmt.setDouble(2, 5.0);
                stmt.setString(3, "available");
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw SqlExceptionTranslator.translate(e, "test insert");
            }
        });
        assertEquals(ConstraintViolationException.ConstraintType.NOT_NULL, ex.getConstraintType());
    }

    @Test
    void outOfRangeStatusViolatesCheckConstraint() {
        Connection connection = db.getConnection();
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class, () -> {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO listed_items (owner_id, item_name, cost_per_day, status) VALUES (?, ?, ?, 'archived')")) {
                stmt.setInt(1, ownerId);
                stmt.setString(2, "Ladder");
                stmt.setDouble(3, 5.0);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw SqlExceptionTranslator.translate(e, "test insert");
            }
        });
        assertEquals(ConstraintViolationException.ConstraintType.CHECK, ex.getConstraintType());
    }
}
