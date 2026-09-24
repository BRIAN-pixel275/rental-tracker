package com.tracker.rentaltracker.repository;

import com.tracker.rentaltracker.domain.User;
import com.tracker.rentaltracker.exception.MappingException;
import com.tracker.rentaltracker.exception.NotFoundException;
import com.tracker.rentaltracker.exception.SqlExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class UserRepository {

    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    public User insert(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return findById(user.getId());
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "insert user " + user.getUsername());
        }
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("No user with id " + id);
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "find user by id " + id);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "find user by username " + username);
        }
    }

    private User mapRow(ResultSet rs) {
        try {
            Integer id = rs.getInt("id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String createdAtRaw = rs.getString("created_at");
            LocalDateTime createdAt = createdAtRaw == null ? null : LocalDateTime.parse(createdAtRaw.replace(" ", "T"));
            return new User(id, username, password, createdAt);
        } catch (SQLException | DateTimeParseException e) {
            throw new MappingException("Failed to map row to User", e);
        }
    }
}
