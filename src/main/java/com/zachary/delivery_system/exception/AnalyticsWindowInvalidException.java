package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class AnalyticsWindowInvalidException extends ApiException {

    public AnalyticsWindowInvalidException() {
        super(
                HttpStatus.BAD_REQUEST,
                "ANALYTICS_WINDOW_INVALID",
                "Minutes must be between 1 and 1440"
        );
    }
}
