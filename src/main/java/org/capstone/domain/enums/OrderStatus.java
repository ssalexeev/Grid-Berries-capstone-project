package org.capstone.domain.enums;

import java.util.Locale;

public enum OrderStatus {
    CREATED,
    IN_PROCESSING,
    SHIPPED,
    COMPLETED,
    CANCELLED;

    public static OrderStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Order status cannot be blank");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order status: " + value);
        }
    }
}
