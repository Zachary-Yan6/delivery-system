package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryOwnerInvalidException extends ApiException {

    public DeliveryOwnerInvalidException() {
        super(
                HttpStatus.BAD_REQUEST,
                "DELIVERY_OWNER_INVALID",
                "Delivery owner must be an existing CUSTOMER account"
        );
    }
}
