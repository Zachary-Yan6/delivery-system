package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryConcurrentUpdateException extends ApiException {

    public DeliveryConcurrentUpdateException() {
        super(
                HttpStatus.CONFLICT,
                "DELIVERY_CONCURRENT_UPDATE",
                "This delivery was changed by another request. Please refresh and try again."
        );
    }
}
