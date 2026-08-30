package com.zachary.delivery_system.dto.Delivery;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDeliveryRequest {

    @NotNull(message = "Driver ID is required")
    private Long driverId;
}