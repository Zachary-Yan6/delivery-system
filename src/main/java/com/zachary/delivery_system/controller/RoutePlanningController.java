package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Route.RoutePlanRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanResponse;
import com.zachary.delivery_system.service.RoutePlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatcher/routes")
@RequiredArgsConstructor
@Tag(name = "Route Planning")
@SecurityRequirement(name = "bearerAuth")

public class RoutePlanningController {

    private final RoutePlanningService routePlanningService;

    @PostMapping
    @Operation(summary = "Build a driving route in the supplied delivery order")
    public RoutePlanResponse planRoute(
            @Valid @RequestBody RoutePlanRequest request
    ) {
        return routePlanningService.plan(request);
    }
}