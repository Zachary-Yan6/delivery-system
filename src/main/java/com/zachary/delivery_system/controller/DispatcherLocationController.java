package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher/driver-locations")
@RequiredArgsConstructor
@Tag(name = "Dispatcher Map", description = "Live driver location APIs")
@SecurityRequirement(name = "bearerAuth")
public class DispatcherLocationController {

    private final DriverLocationService driverLocationService;

    @Operation(summary = "Get the latest location of every active driver")
    @GetMapping
    public List<DriverLatestLocationResponse> getLatestDriverLocations() {
        return driverLocationService.getLatestLocations();
    }
}