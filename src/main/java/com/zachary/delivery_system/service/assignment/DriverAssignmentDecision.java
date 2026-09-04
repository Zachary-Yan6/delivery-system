package com.zachary.delivery_system.service.assignment;

import com.zachary.delivery_system.entity.Driver;

import java.math.BigDecimal;

public record DriverAssignmentDecision(
        Driver driver,
        double score,
        double distanceKm,
        int activeDeliveryCount,
        BigDecimal activeLoadKg
) {
}
