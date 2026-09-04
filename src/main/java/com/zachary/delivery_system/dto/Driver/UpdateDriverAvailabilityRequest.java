package com.zachary.delivery_system.dto.Driver;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDriverAvailabilityRequest {

    @NotNull(message = "Availability is required")
    private Boolean available;
}
