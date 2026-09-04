package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryAccessDeniedException extends ApiException {

    public DeliveryAccessDeniedException() {
        super(
                HttpStatus.FORBIDDEN,
                "DELIVERY_ACCESS_DENIED",
                "This user does not have permission to access this delivery"
        );
    }
}
