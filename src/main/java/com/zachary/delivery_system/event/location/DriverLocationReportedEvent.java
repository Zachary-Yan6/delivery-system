package com.zachary.delivery_system.event.location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverLocationReportedEvent(
        // Let's downstream consumers detect a retry of the same event.
        UUID eventId,
        // Identifies the driver and will be the Kafka message key.
        Long driverId,
        BigDecimal latitude,
        BigDecimal longitude,

        // When the driver device measured the position.
        Instant recordedAt,
        // When your backend accepted it.
        Instant receivedAt
) {
}