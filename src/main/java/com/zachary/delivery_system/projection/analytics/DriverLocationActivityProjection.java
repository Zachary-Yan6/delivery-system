package com.zachary.delivery_system.projection.analytics;

import lombok.Data;

import java.time.Instant;

@Data
public class DriverLocationActivityProjection {

    private Long driverId;

    private String driverName;

    private Long locationCount;

    private Instant lastReceivedAt;
}