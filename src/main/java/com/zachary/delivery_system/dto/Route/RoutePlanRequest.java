package com.zachary.delivery_system.dto.Route;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Data
public class RoutePlanRequest {

    @NotEmpty
    @Size(min = 2, max = 25)
    private List<@NotNull Long> deliveryIds;

    @NotNull
    @Positive
    private Long driverId;
}