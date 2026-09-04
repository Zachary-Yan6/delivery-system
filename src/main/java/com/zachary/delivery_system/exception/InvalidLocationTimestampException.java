package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class InvalidLocationTimestampException extends ApiException {

    public InvalidLocationTimestampException() {
        super(
                HttpStatus.BAD_REQUEST,
                "LOCATION_TIMESTAMP_INVALID",
                "Recorded time cannot be more than five minutes in the future"
        );
    }
}
