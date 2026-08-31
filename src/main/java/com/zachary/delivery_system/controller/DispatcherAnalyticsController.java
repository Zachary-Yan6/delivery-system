package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.service.DispatcherAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher/analytics")
@RequiredArgsConstructor
@Tag(name = "Dispatcher Analytics", description = "Driver location activity APIs")
@SecurityRequirement(name = "bearerAuth")
public class DispatcherAnalyticsController {

    private final DispatcherAnalyticsService dispatcherAnalyticsService;

    @Operation(summary = "Get driver location activity for a time window")
    @GetMapping("/location-activity")
    public List<DriverLocationActivityResponse> getLocationActivity(
            @RequestParam(defaultValue = "60") int minutes
    ) {
        return dispatcherAnalyticsService.getLocationActivity(minutes);
    }
}