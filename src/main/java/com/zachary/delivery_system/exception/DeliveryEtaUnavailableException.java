package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryEtaUnavailableException extends ApiException {

    public DeliveryEtaUnavailableException(String message) {
        super(HttpStatus.CONFLICT, "DELIVERY_ETA_UNAVAILABLE", message);
    }
}
