package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverAccountUnavailableException extends ApiException {

    public DriverAccountUnavailableException() {
        super(
                HttpStatus.FORBIDDEN,
                "DRIVER_ACCOUNT_UNAVAILABLE",
                "Active driver account required"
        );
    }
}
