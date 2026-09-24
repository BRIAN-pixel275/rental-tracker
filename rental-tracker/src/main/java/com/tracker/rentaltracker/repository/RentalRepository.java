package com.tracker.rentaltracker.repository;

import com.tracker.rentaltracker.domain.Rental;
import com.tracker.rentaltracker.domain.RentalStatus;
import com.tracker.rentaltracker.exception.MappingException;
import com.tracker.rentaltracker.exception.NotFoundException;
import com.tracker.rentaltracker.exception.SqlExceptionTranslator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class RentalRepository {

    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Connection connection;

    public RentalRepository(Connection connection) {
        this.connection = connection;
    }

    public Rental insert(Rental rental) {
        String sql = "INSERT INTO rentals (item_id, renter_id, start_time, end_time, returned_at, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, rental.getItemId());
            stmt.setInt(2, rental.getRenterId());
            stmt.setString(3, rental.getStartTime().format(DB_FORMAT));
            stmt.setString(4, rental.getEndTime().format(DB_FORMAT));
            stmt.setString(5, rental.getReturnedAt() == null ? null : rental.getReturnedAt().format(DB_FORMAT));
            stmt.setString(6, rental.getStatus().toDb());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    rental.setRentalId(keys.getInt(1));
                }
            }
            return findById(rental.getRentalId());
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "insert rental for item " + rental.getItemId());
        }
    }

    public Rental findById(int rentalId) {
        String sql = "SELECT * FROM rentals WHERE rental_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, rentalId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("No rental with id " + rentalId);
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "find rental by id " + rentalId);
        }
    }

    /** Active rentals for items owned by ownerId, sorted by due date (soonest first). */
    public List<Rental> findActiveByOwner(int ownerId) {
        String sql = "SELECT r.* FROM rentals r " +
                "JOIN listed_items i ON r.item_id = i.item_id " +
                "WHERE i.owner_id = ? AND r.status = 'active' " +
                "ORDER BY r.end_time ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Rental> rentals = new ArrayList<>();
                while (rs.next()) {
                    rentals.add(mapRow(rs));
                }
                return rentals;
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "query active rentals for owner " + ownerId);
        }
    }

    public boolean hasActiveRentalForItem(int itemId) {
        String sql = "SELECT 1 FROM rentals WHERE item_id = ? AND status = 'active'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "check active rental for item " + itemId);
        }
    }

    public void closeRental(int rentalId, LocalDateTime returnedAt) {
        String sql = "UPDATE rentals SET status = ?, returned_at = ? WHERE rental_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, RentalStatus.CLOSED.toDb());
            stmt.setString(2, returnedAt.format(DB_FORMAT));
            stmt.setInt(3, rentalId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("No rental with id " + rentalId);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "close rental " + rentalId);
        }
    }

    private Rental mapRow(ResultSet rs) {
        try {
            Integer rentalId = rs.getInt("rental_id");
            int itemId = rs.getInt("item_id");
            int renterId = rs.getInt("renter_id");
            LocalDateTime start = parse(rs.getString("start_time"));
            LocalDateTime end = parse(rs.getString("end_time"));
            String returnedRaw = rs.getString("returned_at");
            LocalDateTime returnedAt = returnedRaw == null ? null : parse(returnedRaw);
            RentalStatus status = RentalStatus.fromDb(rs.getString("status"));
            return new Rental(rentalId, itemId, renterId, start, end, returnedAt, status);
        } catch (SQLException | IllegalArgumentException e) {
            throw new MappingException("Failed to map row to Rental", e);
        }
    }

    private LocalDateTime parse(String raw) {
        try {
            return LocalDateTime.parse(raw.replace(" ", "T"));
        } catch (DateTimeParseException e) {
            throw new MappingException("Failed to parse timestamp: " + raw, e);
        }
    }
}
