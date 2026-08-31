package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Location.DriverLocationAlertResponse;
import com.zachary.delivery_system.service.DispatcherAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dispatcher/alerts")
@RequiredArgsConstructor
@Tag(name = "Dispatcher Alerts", description = "Live driver tracking alerts")
@SecurityRequirement(name = "bearerAuth")
public class DispatcherAlertController {

    private final DispatcherAlertService dispatcherAlertService;

    @Operation(summary = "Get active driver tracking alerts")
    @GetMapping
    public List<DriverLocationAlertResponse> getActiveAlerts() {
        return dispatcherAlertService.getActiveAlerts();
    }
}