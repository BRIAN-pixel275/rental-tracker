package com.tracker.rentaltracker.transport;

import com.tracker.rentaltracker.domain.Item;
import com.tracker.rentaltracker.domain.Rental;
import com.tracker.rentaltracker.domain.User;
import com.tracker.rentaltracker.exception.BusinessRuleException;
import com.tracker.rentaltracker.exception.DatabaseException;
import com.tracker.rentaltracker.service.InventoryService;
import com.tracker.rentaltracker.service.RentalService;
import com.tracker.rentaltracker.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class CliApp {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsoleIO io;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final RentalService rentalService;
    private User owner;

    public CliApp(UserService userService, InventoryService inventoryService, RentalService rentalService) {
        this.io = new ConsoleIO(new Scanner(System.in));
        this.userService = userService;
        this.inventoryService = inventoryService;
        this.rentalService = rentalService;
    }

    public void run() {
        owner = userService.loadOrCreateOwner(() -> io.prompt("Welcome! Choose a username: "));
        io.print("Welcome back, " + owner.getUsername() + "!");

        boolean running = true;
        while (running) {
            try {
                running = mainMenu();
            } catch (BusinessRuleException | DatabaseException e) {
                io.print("Error: " + e.getMessage());
            }
        }
    }

    private boolean mainMenu() {
        io.print("\n=== Rental tracker ===");
        io.print("1) List an item");
        io.print("2) View my inventory");
        io.print("3) Record a rental");
        io.print("4) Confirm a return");
        io.print("5) Exit");
        int choice = io.promptInt("> ");
        switch (choice) {
            case 1 -> listItem();
            case 2 -> viewInventory();
            case 3 -> recordRental();
            case 4 -> confirmReturn();
            case 5 -> { return false; }
            default -> io.print("Pick an option from the menu.");
        }
        return true;
    }

    private void listItem() {
        String name = io.prompt("Item name: ");
        String description = io.prompt("Description: ");
        double cost = io.promptDouble("Cost per day: ");
        Item item = inventoryService.listItem(owner.getId(), name, description, cost);
        io.print("Listed \"" + item.getItemName() + "\" as available.");
    }

    private void viewInventory() {
        List<Item> items = inventoryService.getInventory(owner.getId());
        if (items.isEmpty()) {
            io.print("You haven't listed anything yet.");
            return;
        }
        Paginator<Item> pages = new Paginator<>(items,
                item -> io.printf("%s    %s%n", item.getItemName(), item.getStatus().toDb()));
        paginatedSelection(pages, this::itemDetail);
    }

    private void itemDetail(Item item) {
        io.print("\n=== " + item.getItemName() + " ===");
        io.print("description   " + item.getDescription());
        io.print("cost per day  " + item.getCostPerDay());
        io.print("status        " + item.getStatus().toDb());
        io.print("owner         " + owner.getUsername());
        io.print("listed        " + (item.getCreatedAt() == null ? "-" : item.getCreatedAt().format(DISPLAY_FORMAT)));

        switch (item.getStatus()) {
            case AVAILABLE -> {
                io.print("1) Delist");
                io.print("2) Back to list");
                if (io.promptInt("> ") == 1) {
                    inventoryService.delist(item.getItemId());
                    io.print("Delisted.");
                }
            }
            case UNLISTED -> {
                io.print("1) Relist");
                io.print("2) Back to list");
                if (io.promptInt("> ") == 1) {
                    inventoryService.relist(item.getItemId());
                    io.print("Relisted as available.");
                }
            }
            case RENTED -> {
                io.print("This item is currently out on rental.");
                io.print("1) Delist (it will stay unlisted once returned)");
                io.print("2) Back to list");
                if (io.promptInt("> ") == 1) {
                    inventoryService.delist(item.getItemId());
                    io.print("Delisted — it will not go back to available when returned.");
                }
            }
        }
    }

    private void recordRental() {
        List<Item> available = inventoryService.getAvailableItems(owner.getId());
        if (available.isEmpty()) {
            io.print("Nothing available to rent out right now.");
            return;
        }
        Paginator<Item> pages = new Paginator<>(available,
                item -> io.printf("%s    %.2f/day%n", item.getItemName(), item.getCostPerDay()));
        paginatedSelection(pages, selected -> {
            String renterName = io.prompt("Renter's name: ");
            int duration = io.promptInt("Duration (days): ");
            Rental rental = rentalService.recordRental(selected.getItemId(), renterName, duration);
            io.print("Rented \"" + selected.getItemName() + "\" to " + renterName
                    + " until " + rental.getEndTime().format(DISPLAY_FORMAT) + ".");
        });
    }

    private void confirmReturn() {
        List<Rental> active = rentalService.getActiveRentals(owner.getId());
        if (active.isEmpty()) {
            io.print("Nothing out on rental right now.");
            return;
        }
        Paginator<Rental> pages = new Paginator<>(active,
                rental -> {
                    Item item = inventoryService.getItem(rental.getItemId());
                    io.printf("%s    due %s%n", item.getItemName(), rental.getEndTime().format(DISPLAY_FORMAT));
                });
        paginatedSelection(pages, this::rentalDetail);
    }

    private void rentalDetail(Rental rental) {
        Item item = inventoryService.getItem(rental.getItemId());
        io.print("\n=== " + item.getItemName() + " ===");
        io.print("start    " + rental.getStartTime().format(DISPLAY_FORMAT));
        io.print("end      " + rental.getEndTime().format(DISPLAY_FORMAT));
        io.print("status   " + rental.getStatus().toDb());
        io.print("1) Confirm return");
        io.print("2) Back to list");
        if (io.promptInt("> ") == 1) {
            rentalService.confirmReturn(rental.getRentalId());
            io.print("Return confirmed.");
        }
    }

    /** Shared next/previous/back loop for any paginated selection screen. */
    private <T> void paginatedSelection(Paginator<T> pages, Consumer<T> onSelect) {
        while (true) {
            List<T> pageItems = pages.currentPageItems();
            pages.render(io);
            int choice = io.promptInt("> ");

            int rowCount = pageItems.size();
            if (choice >= 1 && choice <= rowCount) {
                onSelect.accept(pageItems.get(choice - 1));
                return;
            }

            int next = rowCount + 1;
            int previous = pages.hasNext() ? rowCount + 2 : rowCount + 1;
            int back;
            if (pages.hasNext() && pages.hasPrevious()) {
                back = rowCount + 3;
            } else if (pages.hasNext() || pages.hasPrevious()) {
                back = rowCount + 2;
            } else {
                back = rowCount + 1;
            }

            if (pages.hasNext() && choice == next) {
                pages.next();
            } else if (pages.hasPrevious() && choice == previous) {
                pages.previous();
            } else if (choice == back) {
                return;
            } else {
                io.print("Pick an option from the menu.");
            }
        }
    }
}
