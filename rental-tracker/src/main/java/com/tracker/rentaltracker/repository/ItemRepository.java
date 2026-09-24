package com.tracker.rentaltracker.repository;

import com.tracker.rentaltracker.domain.Item;
import com.tracker.rentaltracker.domain.ItemStatus;
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
import java.util.ArrayList;
import java.util.List;

public class ItemRepository {

    private final Connection connection;

    public ItemRepository(Connection connection) {
        this.connection = connection;
    }

    public Item insert(Item item) {
        String sql = "INSERT INTO listed_items (owner_id, item_name, description, cost_per_day, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getOwnerId());
            stmt.setString(2, item.getItemName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getCostPerDay());
            stmt.setString(5, item.getStatus().toDb());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setItemId(keys.getInt(1));
                }
            }
            return findById(item.getItemId());
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "insert item " + item.getItemName());
        }
    }

    public Item findById(int itemId) {
        String sql = "SELECT * FROM listed_items WHERE item_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("No item with id " + itemId);
                }
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "find item by id " + itemId);
        }
    }

    public List<Item> findByOwner(int ownerId) {
        return queryItems("SELECT * FROM listed_items WHERE owner_id = ? ORDER BY item_id", ownerId);
    }

    public List<Item> findAvailableByOwner(int ownerId) {
        return queryItems("SELECT * FROM listed_items WHERE owner_id = ? AND status = 'available' ORDER BY item_id", ownerId);
    }

    private List<Item> queryItems(String sql, int ownerId) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ownerId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "query items for owner " + ownerId);
        }
    }

    public void updateStatus(int itemId, ItemStatus status) {
        String sql = "UPDATE listed_items SET status = ? WHERE item_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.toDb());
            stmt.setInt(2, itemId);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new NotFoundException("No item with id " + itemId);
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate(e, "update status for item " + itemId);
        }
    }

    private Item mapRow(ResultSet rs) {
        try {
            Integer itemId = rs.getInt("item_id");
            int ownerId = rs.getInt("owner_id");
            String name = rs.getString("item_name");
            String description = rs.getString("description");
            double costPerDay = rs.getDouble("cost_per_day");
            ItemStatus status = ItemStatus.fromDb(rs.getString("status"));
            String createdAtRaw = rs.getString("created_at");
            LocalDateTime createdAt = createdAtRaw == null ? null : LocalDateTime.parse(createdAtRaw.replace(" ", "T"));
            return new Item(itemId, ownerId, name, description, costPerDay, status, createdAt);
        } catch (SQLException | IllegalArgumentException | DateTimeParseException e) {
            throw new MappingException("Failed to map row to Item", e);
        }
    }
}
