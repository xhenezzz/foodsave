package com.example.FoodSave.auth.entity;

public enum City {

    ALMATY("Almaty"),
    ASTANA("Astana");

    private final String displayName;

    City(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}