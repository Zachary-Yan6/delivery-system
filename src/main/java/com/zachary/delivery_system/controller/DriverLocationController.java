package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Location.DriverLocationRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver/locations")
@RequiredArgsConstructor
@Tag(name = "Driver Locations", description = "Driver location tracking APIs")
@SecurityRequirement(name = "bearerAuth")
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @Operation(summary = "Post my current location")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverLocation postLocation(
            @AuthenticationPrincipal AppUser currentUser,
            @Valid @RequestBody DriverLocationRequest request
    ) {
        return driverLocationService.recordLocation(currentUser, request);
    }
}