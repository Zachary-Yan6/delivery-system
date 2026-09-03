package com.zachary.delivery_system.dto.Location;

import java.time.Instant;

public record DriverLocationActivityResponse(
        Long driverId,
        String driverName,
        Long locationCount,
        Instant lastReceivedAt,
        boolean online
) {
}