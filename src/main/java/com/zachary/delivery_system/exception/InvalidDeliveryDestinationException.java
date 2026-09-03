package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class InvalidDeliveryDestinationException extends ApiException {

    public InvalidDeliveryDestinationException() {
        super(
                HttpStatus.BAD_REQUEST,
                "DELIVERY_DESTINATION_INVALID",
                "Both destination latitude and longitude are required"
        );
    }
}
