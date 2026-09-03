package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverNotFoundException extends ApiException {

    public DriverNotFoundException(Long driverId) {
        super(
                HttpStatus.NOT_FOUND,
                "DRIVER_NOT_FOUND",
                "Driver not found: " + driverId
        );
    }
}
