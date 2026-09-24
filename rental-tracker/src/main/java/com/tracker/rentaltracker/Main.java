package com.tracker.rentaltracker;

import com.tracker.rentaltracker.infrastructure.DatabaseManager;
import com.tracker.rentaltracker.infrastructure.OwnerConfig;
import com.tracker.rentaltracker.repository.ItemRepository;
import com.tracker.rentaltracker.repository.RentalRepository;
import com.tracker.rentaltracker.repository.UserRepository;
import com.tracker.rentaltracker.service.InventoryService;
import com.tracker.rentaltracker.service.RentalService;
import com.tracker.rentaltracker.service.UserService;
import com.tracker.rentaltracker.transport.CliApp;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        DatabaseManager db = new DatabaseManager("rental-tracker.db");
        db.connect();

        OwnerConfig ownerConfig = new OwnerConfig(Path.of("owner.cfg"));

        boolean seed = args.length > 0 && args[0].equals("--seed");
        if (seed) {
            db.loadSampleData();
            // sample-data.sql's owner row is always user id 1 (liisa) — point the
            // app at that account so seeded items actually show up as "yours".
            ownerConfig.writeOwnerId(1);
            System.out.println("Sample data loaded — you are now 'liisa' (owner id 1).");
        }

        try {
            UserRepository userRepository = new UserRepository(db.getConnection());
            ItemRepository itemRepository = new ItemRepository(db.getConnection());
            RentalRepository rentalRepository = new RentalRepository(db.getConnection());

            UserService userService = new UserService(userRepository, ownerConfig);
            InventoryService inventoryService = new InventoryService(itemRepository, rentalRepository);
            RentalService rentalService = new RentalService(itemRepository, rentalRepository, userService);

            new CliApp(userService, inventoryService, rentalService).run();
        } finally {
            db.close();
        }
    }
}
