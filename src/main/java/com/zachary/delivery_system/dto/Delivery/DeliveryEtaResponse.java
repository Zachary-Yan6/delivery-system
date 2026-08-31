package com.zachary.delivery_system.dto.Delivery;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class DeliveryEtaResponse {

    private Long deliveryId;
    private Long driverId;
    private double distanceMeters;
    private double fixedSpeedKph;
    private long durationSeconds;
    private Instant estimatedArrivalAt;
    private Instant calculatedAt;
}