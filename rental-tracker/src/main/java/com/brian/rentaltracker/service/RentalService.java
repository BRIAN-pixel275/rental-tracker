package com.brian.rentaltracker.service;

import com.brian.rentaltracker.domain.Item;
import com.brian.rentaltracker.domain.ItemStatus;
import com.brian.rentaltracker.domain.Rental;
import com.brian.rentaltracker.domain.RentalStatus;
import com.brian.rentaltracker.domain.User;
import com.brian.rentaltracker.exception.BusinessRuleException;
import com.brian.rentaltracker.repository.ItemRepository;
import com.brian.rentaltracker.repository.RentalRepository;

import java.time.LocalDateTime;
import java.util.List;

public class RentalService {

    private final ItemRepository itemRepository;
    private final RentalRepository rentalRepository;
    private final UserService userService;

    public RentalService(ItemRepository itemRepository, RentalRepository rentalRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.rentalRepository = rentalRepository;
        this.userService = userService;
    }

    public Rental recordRental(int itemId, String renterUsername, int durationDays) {
        Item item = itemRepository.findById(itemId);
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new BusinessRuleException("Item is not available to rent (current status: " + item.getStatus() + ").");
        }
        User renter = userService.findOrCreateRenter(renterUsername);

        Rental rental = Rental.start(itemId, renter.getId(), LocalDateTime.now(), durationDays);
        Rental saved = rentalRepository.insert(rental);

        itemRepository.updateStatus(itemId, ItemStatus.RENTED);
        return saved;
    }

    public List<Rental> getActiveRentals(int ownerId) {
        return rentalRepository.findActiveByOwner(ownerId);
    }

    public void confirmReturn(int rentalId) {
        Rental rental = rentalRepository.findById(rentalId);
        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessRuleException("Rental " + rentalId + " is not active.");
        }
        rentalRepository.closeRental(rentalId, LocalDateTime.now());

        Item item = itemRepository.findById(rental.getItemId());
        // If the item was delisted while it was out, leave it unlisted; otherwise it goes back to available.
        if (item.getStatus() != ItemStatus.UNLISTED) {
            itemRepository.updateStatus(item.getItemId(), ItemStatus.AVAILABLE);
        }
    }
}
