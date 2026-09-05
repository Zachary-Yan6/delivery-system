package com.zachary.delivery_system.service.assignment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.zachary.delivery_system.entity.Driver;

import java.math.BigDecimal;

@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "The chosen mutable Driver is intentionally returned so the assignment workflow can reserve it."
)
public record DriverAssignmentDecision(
        Driver driver,
        double score,
        double distanceKm,
        int activeDeliveryCount,
        BigDecimal activeLoadKg
) {

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "The chosen mutable Driver is intentionally retained for the assignment workflow."
    )
    public DriverAssignmentDecision {
    }
}
