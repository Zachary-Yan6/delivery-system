package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class LocationUpdateRateLimitException extends ApiException {

    public LocationUpdateRateLimitException() {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                "LOCATION_UPDATE_RATE_LIMITED",
                "Location updates may be sent only once every five seconds"
        );
    }
}
