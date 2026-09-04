package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class NoEligibleDriverException extends ApiException {

    public NoEligibleDriverException() {
        super(
                HttpStatus.CONFLICT,
                "NO_ELIGIBLE_DRIVER",
                "No available driver currently satisfies location, workload, and vehicle capacity requirements"
        );
    }
}
