package com.zachary.delivery_system.dto.Driver;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDriverRequest {

    @NotBlank(message = "Driver name is required")
    private String fullName;

    @NotBlank(message = "Phone is required")
    private String phone;
}