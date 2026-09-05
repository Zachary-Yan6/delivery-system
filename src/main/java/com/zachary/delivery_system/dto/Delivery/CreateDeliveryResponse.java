package com.zachary.delivery_system.dto.Delivery;

import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.enums.DeliveryStatus;

import java.time.Instant;

/**
 * Stable API response returned after a delivery is created.
 *
 * <p>Keeping this separate from {@link Delivery} prevents database entity
 * fields from becoming part of the public HTTP contract.</p>
 */
public record CreateDeliveryResponse(
        Long id,
        DeliveryStatus status,
        Instant createdAt
) {

    public static CreateDeliveryResponse from(Delivery delivery) {
        Instant createdAt = delivery.getCreatedAt() == null
                ? null
                : delivery.getCreatedAt().toInstant();

        return new CreateDeliveryResponse(
                delivery.getId(),
                delivery.getStatus(),
                createdAt
        );
    }
}
