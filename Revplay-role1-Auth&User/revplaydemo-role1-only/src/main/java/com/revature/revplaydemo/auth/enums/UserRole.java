package com.revature.revplaydemo.auth.enums;

public enum UserRole {
    LISTENER,
    ARTIST,
    ADMIN;

    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            return LISTENER;
        }
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return LISTENER;
        }
    }
}
