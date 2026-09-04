package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryTimeWindowExpiredException extends ApiException {

    public DeliveryTimeWindowExpiredException(Long deliveryId) {
        super(
                HttpStatus.CONFLICT,
                "DELIVERY_TIME_WINDOW_EXPIRED",
                "Delivery #" + deliveryId + " has an expired delivery time window"
        );
    }
}
