package com.tracker.rentaltracker.domain;

import java.time.LocalDateTime;

public class Rental {
    private Integer rentalId;
    private final int itemId;
    private final int renterId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private LocalDateTime returnedAt;
    private RentalStatus status;

    public Rental(Integer rentalId, int itemId, int renterId, LocalDateTime startTime,
                  LocalDateTime endTime, LocalDateTime returnedAt, RentalStatus status) {
        this.rentalId = rentalId;
        this.itemId = itemId;
        this.renterId = renterId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.returnedAt = returnedAt;
        this.status = status;
    }

    public static Rental start(int itemId, int renterId, LocalDateTime startTime, int durationDays) {
        return new Rental(null, itemId, renterId, startTime, startTime.plusDays(durationDays), null, RentalStatus.ACTIVE);
    }

    public Integer getRentalId() { return rentalId; }
    public void setRentalId(Integer rentalId) { this.rentalId = rentalId; }
    public int getItemId() { return itemId; }
    public int getRenterId() { return renterId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }
}
