package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class RouteDeliveryNotFoundException extends ApiException {

    public RouteDeliveryNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "ROUTE_DELIVERY_NOT_FOUND",
                "One or more selected deliveries do not exist."
        );
    }
}
