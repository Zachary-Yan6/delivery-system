package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverInactiveException extends ApiException {

    public DriverInactiveException() {
        super(
                HttpStatus.CONFLICT,
                "DRIVER_INACTIVE",
                "Cannot assign a delivery to an inactive driver"
        );
    }
}
