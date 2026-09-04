package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverLocationAccessDeniedException extends ApiException {

    public DriverLocationAccessDeniedException() {
        super(
                HttpStatus.FORBIDDEN,
                "DRIVER_LOCATION_ACCESS_DENIED",
                "An active driver account is required"
        );
    }
}
