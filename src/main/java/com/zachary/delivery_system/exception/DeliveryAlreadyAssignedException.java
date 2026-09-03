package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryAlreadyAssignedException extends ApiException {

    public DeliveryAlreadyAssignedException() {
        super(
                HttpStatus.CONFLICT,
                "DELIVERY_ALREADY_ASSIGNED",
                "Delivery has already been assigned"
        );
    }
}
