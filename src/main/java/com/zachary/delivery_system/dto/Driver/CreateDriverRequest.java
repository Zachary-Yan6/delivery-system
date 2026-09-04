package com.zachary.delivery_system.dto.Driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDriverRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    @NotBlank(message = "Driver name is required")
    private String fullName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @DecimalMin(value = "0.01", message = "Vehicle capacity must be greater than zero")
    private BigDecimal vehicleCapacityKg = new BigDecimal("100.00");

    private Boolean available = true;
}
