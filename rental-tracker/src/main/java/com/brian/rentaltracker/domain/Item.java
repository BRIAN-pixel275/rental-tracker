package com.brian.rentaltracker.domain;

import java.time.LocalDateTime;

public class Item {
    private Integer itemId;
    private final int ownerId;
    private String itemName;
    private String description;
    private double costPerDay;
    private ItemStatus status;
    private LocalDateTime createdAt;

    public Item(Integer itemId, int ownerId, String itemName, String description,
                double costPerDay, ItemStatus status, LocalDateTime createdAt) {
        this.itemId = itemId;
        this.ownerId = ownerId;
        this.itemName = itemName;
        this.description = description;
        this.costPerDay = costPerDay;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Item newListing(int ownerId, String name, String description, double costPerDay) {
        return new Item(null, ownerId, name, description, costPerDay, ItemStatus.AVAILABLE, null);
    }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public int getOwnerId() { return ownerId; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public double getCostPerDay() { return costPerDay; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
