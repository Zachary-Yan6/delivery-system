package com.zachary.delivery_system.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * The valid states in a delivery's lifecycle.
 *
 * <p>The enum names are stored in PostgreSQL as text, for example
 * {@code IN_TRANSIT}. The database constraint and frontend API values must use
 * these exact enum names.</p>
 */
public enum DeliveryStatus {

    CREATED,
    ASSIGNED,
    ACCEPTED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    RETRY,
    COMPLETED,
    CANCELLED,
    RETURNED;

    public boolean canAssign() {
        return this == CREATED;
    }

    public boolean canStartTransit() {
        return this == PICKED_UP;
    }

    public boolean canDeliver() {
        return this == IN_TRANSIT;
    }

    public boolean canFail() {
        return this == PICKED_UP || this == IN_TRANSIT || this == RETRY;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == RETURNED;
    }

    public boolean countsTowardDriverWorkload() {
        return this == ASSIGNED
                || this == ACCEPTED
                || this == PICKED_UP
                || this == IN_TRANSIT
                || this == RETRY;
    }

    public boolean canBeAddedToRoute() {
        return countsTowardDriverWorkload();
    }

    public boolean canTransitionTo(DeliveryStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }

        return switch (this) {
            case CREATED -> nextStatus == ASSIGNED
                    || nextStatus == CANCELLED;
            case ASSIGNED -> nextStatus == ACCEPTED
                    || nextStatus == CANCELLED;
            case ACCEPTED -> nextStatus == PICKED_UP
                    || nextStatus == CANCELLED;
            case PICKED_UP -> nextStatus == IN_TRANSIT
                    || nextStatus == FAILED
                    || nextStatus == CANCELLED;
            case IN_TRANSIT -> nextStatus == DELIVERED
                    || nextStatus == FAILED
                    || nextStatus == CANCELLED;
            case FAILED -> nextStatus == RETRY
                    || nextStatus == RETURNED
                    || nextStatus == CANCELLED;
            case RETRY -> nextStatus == IN_TRANSIT
                    || nextStatus == FAILED
                    || nextStatus == CANCELLED;
            case DELIVERED -> nextStatus == COMPLETED
                    || nextStatus == RETURNED;
            case COMPLETED, CANCELLED, RETURNED -> false;
        };
    }

    public static Set<DeliveryStatus> driverWorkloadStatuses() {
        return EnumSet.of(
                ASSIGNED,
                ACCEPTED,
                PICKED_UP,
                IN_TRANSIT,
                RETRY
        );
    }
}
