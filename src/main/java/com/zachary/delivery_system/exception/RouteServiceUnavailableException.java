package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class RouteServiceUnavailableException extends ApiException {

    public RouteServiceUnavailableException() {
        super(
                HttpStatus.BAD_GATEWAY,
                "ROUTE_SERVICE_UNAVAILABLE",
                "The route service is temporarily unavailable."
        );
    }
}
