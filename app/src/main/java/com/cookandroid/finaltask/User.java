package com.cookandroid.finaltask;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String userId;
    private String password;
    private String name;
    private String phone;
    private List<String> savedLocations;
    private long createdAt;

    public User() {
        this.savedLocations = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, String password, String name, String phone) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.savedLocations = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getSavedLocations() {
        return savedLocations;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setSavedLocations(List<String> savedLocations) {
        this.savedLocations = savedLocations;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public void addLocation(String location) {
        if (!savedLocations.contains(location)) {
            savedLocations.add(location);
        }
    }

    public void removeLocation(String location) {
        savedLocations.remove(location);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", savedLocations=" + savedLocations +
                ", createdAt=" + createdAt +
                '}';
    }
}