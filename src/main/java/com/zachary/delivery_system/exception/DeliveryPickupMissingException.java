package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryPickupMissingException extends ApiException {

    public DeliveryPickupMissingException(Long deliveryId) {
        super(
                HttpStatus.BAD_REQUEST,
                "DELIVERY_PICKUP_MISSING",
                "Delivery #" + deliveryId + " does not have a pickup location"
        );
    }
}
