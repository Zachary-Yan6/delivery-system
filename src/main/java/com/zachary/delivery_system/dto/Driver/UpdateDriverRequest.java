package com.zachary.delivery_system.dto.Driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDriverRequest {

    @NotBlank(message = "Driver name is required")
    private String fullName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @DecimalMin(value = "0.01", message = "Vehicle capacity must be greater than zero")
    private BigDecimal vehicleCapacityKg;

    private Boolean available;
}
