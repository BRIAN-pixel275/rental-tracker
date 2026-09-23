package com.brian.rentaltracker.domain;

import java.time.LocalDateTime;

public class User {
    private Integer id;
    private final String username;
    private final String password;
    private LocalDateTime createdAt;

    public User(Integer id, String username, String password, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    public static User newUser(String username) {
        return new User(null, username, null, null);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
