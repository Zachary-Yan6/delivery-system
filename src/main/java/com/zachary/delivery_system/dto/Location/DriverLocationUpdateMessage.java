package com.zachary.delivery_system.dto.Location;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Small WebSocket contract for one accepted driver location update.
 * Driver names come from the initial REST snapshot, so high-frequency events
 * do not need an extra database query just to enrich every message.
 */
public record DriverLocationUpdateMessage(
        Long driverId,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant recordedAt,
        Instant receivedAt
) {
}
