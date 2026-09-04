package com.zachary.delivery_system.enums;

public enum DeliveryPriority {

    LOW(0.75),
    NORMAL(1.0),
    HIGH(1.5),
    URGENT(2.0);

    private final double distanceWeight;

    DeliveryPriority(double distanceWeight) {
        this.distanceWeight = distanceWeight;
    }

    public double getDistanceWeight() {
        return distanceWeight;
    }
}
