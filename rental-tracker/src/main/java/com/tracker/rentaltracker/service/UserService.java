package com.tracker.rentaltracker.service;

import com.tracker.rentaltracker.domain.User;
import com.tracker.rentaltracker.infrastructure.OwnerConfig;
import com.tracker.rentaltracker.repository.UserRepository;

import java.util.function.Supplier;

public class UserService {

    private final UserRepository userRepository;
    private final OwnerConfig ownerConfig;

    public UserService(UserRepository userRepository, OwnerConfig ownerConfig) {
        this.userRepository = userRepository;
        this.ownerConfig = ownerConfig;
    }

    /**
     * On first launch, creates and persists the owner account.
     * On every launch after, loads that same account instead of asking again.
     */
    public User loadOrCreateOwner(Supplier<String> usernamePrompt) {
        return ownerConfig.readOwnerId()
                .map(userRepository::findById)
                .orElseGet(() -> {
                    User created = userRepository.insert(User.newUser(usernamePrompt.get()));
                    ownerConfig.writeOwnerId(created.getId());
                    return created;
                });
    }

    /** Finds a renter by username, creating them the first time they're seen; reused after that. */
    public User findOrCreateRenter(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.insert(User.newUser(username)));
    }
}
