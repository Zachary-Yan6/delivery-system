package com.zachary.delivery_system.exception;

import org.springframework.http.HttpStatus;

public class DeliveryAssignmentNotAllowedException extends ApiException {

    public DeliveryAssignmentNotAllowedException() {
        super(
                HttpStatus.CONFLICT,
                "DELIVERY_ASSIGNMENT_NOT_ALLOWED",
                "Only a CREATED delivery can be assigned"
        );
    }
}
