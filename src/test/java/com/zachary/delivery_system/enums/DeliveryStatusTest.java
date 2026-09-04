package com.zachary.delivery_system.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryStatusTest {

    @Test
    void lifecycleTransitionsAreAllowedOnlyFromTheirExpectedState() {
        assertTrue(DeliveryStatus.CREATED.canTransitionTo(DeliveryStatus.ASSIGNED));
        assertTrue(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.ACCEPTED));
        assertTrue(DeliveryStatus.ACCEPTED.canTransitionTo(DeliveryStatus.PICKED_UP));
        assertTrue(DeliveryStatus.PICKED_UP.canTransitionTo(DeliveryStatus.IN_TRANSIT));
        assertTrue(DeliveryStatus.IN_TRANSIT.canTransitionTo(DeliveryStatus.DELIVERED));
        assertTrue(DeliveryStatus.DELIVERED.canTransitionTo(DeliveryStatus.COMPLETED));

        assertFalse(DeliveryStatus.CREATED.canTransitionTo(DeliveryStatus.DELIVERED));
        assertFalse(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.IN_TRANSIT));
        assertFalse(DeliveryStatus.COMPLETED.canTransitionTo(DeliveryStatus.IN_TRANSIT));
    }

    @Test
    void aFailedDeliveryCanBeRetriedAndResumed() {
        assertTrue(DeliveryStatus.IN_TRANSIT.canTransitionTo(DeliveryStatus.FAILED));
        assertTrue(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.RETRY));
        assertTrue(DeliveryStatus.RETRY.canTransitionTo(DeliveryStatus.IN_TRANSIT));
        assertFalse(DeliveryStatus.FAILED.isTerminal());
    }

    @Test
    void exceptionalTransitionsFollowTheBusinessRules() {
        assertTrue(DeliveryStatus.CREATED.canTransitionTo(DeliveryStatus.CANCELLED));
        assertTrue(DeliveryStatus.PICKED_UP.canTransitionTo(DeliveryStatus.FAILED));
        assertTrue(DeliveryStatus.DELIVERED.canTransitionTo(DeliveryStatus.RETURNED));
        assertTrue(DeliveryStatus.FAILED.canTransitionTo(DeliveryStatus.RETURNED));

        assertFalse(DeliveryStatus.DELIVERED.canTransitionTo(DeliveryStatus.CANCELLED));
        assertFalse(DeliveryStatus.RETURNED.canTransitionTo(DeliveryStatus.RETRY));
    }

    @Test
    void completedCancelledAndReturnedDeliveriesAreTerminal() {
        assertTrue(DeliveryStatus.COMPLETED.isTerminal());
        assertTrue(DeliveryStatus.CANCELLED.isTerminal());
        assertTrue(DeliveryStatus.RETURNED.isTerminal());
        assertFalse(DeliveryStatus.DELIVERED.isTerminal());
        assertFalse(DeliveryStatus.IN_TRANSIT.isTerminal());
    }
}
