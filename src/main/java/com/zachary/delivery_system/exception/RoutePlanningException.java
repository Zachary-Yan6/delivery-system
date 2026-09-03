package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class RoutePlanningException extends ApiException {

    public RoutePlanningException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }
}
