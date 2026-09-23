package com.brian.rentaltracker.repository;

import com.brian.rentaltracker.domain.User;
import com.brian.rentaltracker.exception.ConstraintViolationException;
import com.brian.rentaltracker.exception.NotFoundException;
import com.brian.rentaltracker.infrastructure.DatabaseManager;
import com.brian.rentaltracker.testsupport.TestDatabaseFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest {

    private DatabaseManager db;
    private UserRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        db = TestDatabaseFactory.freshDatabase(tempDir);
        repository = new UserRepository(db.getConnection());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void insertsAndFindsUserById() {
        User inserted = repository.insert(User.newUser("liisa"));
        assertNotNull(inserted.getId());

        User found = repository.findById(inserted.getId());
        assertEquals("liisa", found.getUsername());
    }

    @Test
    void findsUserByUsername() {
        repository.insert(User.newUser("meelis"));
        assertTrue(repository.findByUsername("meelis").isPresent());
        assertTrue(repository.findByUsername("nobody").isEmpty());
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        assertThrows(NotFoundException.class, () -> repository.findById(999));
    }

    @Test
    void duplicateUsernameViolatesUniqueConstraint() {
        repository.insert(User.newUser("liisa"));
        ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
                () -> repository.insert(User.newUser("liisa")));
        assertEquals(ConstraintViolationException.ConstraintType.UNIQUE, ex.getConstraintType());
    }
}
