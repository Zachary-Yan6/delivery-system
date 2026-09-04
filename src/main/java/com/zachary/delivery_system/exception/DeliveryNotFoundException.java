package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryNotFoundException extends ApiException {

    public DeliveryNotFoundException(Long deliveryId) {
        super(
                HttpStatus.NOT_FOUND,
                "DELIVERY_NOT_FOUND",
                "Delivery not found: " + deliveryId
        );
    }
}
