package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverHasActiveDeliveriesException extends ApiException {

    public DriverHasActiveDeliveriesException() {
        super(
                HttpStatus.CONFLICT,
                "DRIVER_HAS_ACTIVE_DELIVERIES",
                "Cannot deactivate a driver with active deliveries"
        );
    }
}
