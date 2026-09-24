package com.tracker.rentaltracker.service;

import com.tracker.rentaltracker.domain.Item;
import com.tracker.rentaltracker.domain.ItemStatus;
import com.tracker.rentaltracker.exception.BusinessRuleException;
import com.tracker.rentaltracker.repository.ItemRepository;
import com.tracker.rentaltracker.repository.RentalRepository;

import java.util.List;

public class InventoryService {

    private final ItemRepository itemRepository;
    private final RentalRepository rentalRepository;

    public InventoryService(ItemRepository itemRepository, RentalRepository rentalRepository) {
        this.itemRepository = itemRepository;
        this.rentalRepository = rentalRepository;
    }

    public Item listItem(int ownerId, String name, String description, double costPerDay) {
        return itemRepository.insert(Item.newListing(ownerId, name, description, costPerDay));
    }

    public Item getItem(int itemId) {
        return itemRepository.findById(itemId);
    }

    public List<Item> getInventory(int ownerId) {
        return itemRepository.findByOwner(ownerId);
    }

    public List<Item> getAvailableItems(int ownerId) {
        return itemRepository.findAvailableByOwner(ownerId);
    }

    public void delist(int itemId) {
        Item item = itemRepository.findById(itemId);
        transitionOrThrow(item, ItemStatus.UNLISTED);
        itemRepository.updateStatus(itemId, ItemStatus.UNLISTED);
    }

    public void relist(int itemId) {
        Item item = itemRepository.findById(itemId);
        if (item.getStatus() != ItemStatus.UNLISTED) {
            throw new BusinessRuleException("Only unlisted items can be relisted.");
        }
        if (rentalRepository.hasActiveRentalForItem(itemId)) {
            throw new BusinessRuleException("This item is still out on a rental — confirm the return before relisting.");
        }
        transitionOrThrow(item, ItemStatus.AVAILABLE);
        itemRepository.updateStatus(itemId, ItemStatus.AVAILABLE);
    }

    private void transitionOrThrow(Item item, ItemStatus target) {
        if (!item.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException("Cannot move item from " + item.getStatus() + " to " + target);
        }
    }
}
