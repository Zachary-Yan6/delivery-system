package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DriverConcurrentAssignmentException extends ApiException {

    public DriverConcurrentAssignmentException() {
        super(
                HttpStatus.CONFLICT,
                "DRIVER_CONCURRENT_ASSIGNMENT",
                "The selected driver's workload changed. Please run assignment again"
        );
    }
}
