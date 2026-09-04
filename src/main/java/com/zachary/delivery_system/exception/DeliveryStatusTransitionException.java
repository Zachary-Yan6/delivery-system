package com.zachary.delivery_system.exception;

import com.zachary.delivery_system.enums.DeliveryStatus;
import org.springframework.http.HttpStatus;

public class DeliveryStatusTransitionException extends ApiException {

    public DeliveryStatusTransitionException(
            DeliveryStatus currentStatus,
            DeliveryStatus requestedStatus
    ) {
        super(
                HttpStatus.CONFLICT,
                "DELIVERY_STATUS_TRANSITION_NOT_ALLOWED",
                "Delivery cannot transition from "
                        + currentStatus
                        + " to "
                        + requestedStatus
        );
    }
}
