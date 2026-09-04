package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryDestinationMissingException extends ApiException {

    public DeliveryDestinationMissingException(Long deliveryId) {
        super(
                HttpStatus.BAD_REQUEST,
                "DELIVERY_DESTINATION_MISSING",
                "Delivery #" + deliveryId + " does not have a destination pin."
        );
    }
}
