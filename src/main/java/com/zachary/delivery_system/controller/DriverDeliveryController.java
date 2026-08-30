package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/driver/deliveries")
@RequiredArgsConstructor
@Tag(name = "Driver Deliveries", description = "Driver delivery workflow APIs")
@SecurityRequirement(name = "bearerAuth")
public class DriverDeliveryController {

    private final DeliveryService deliveryService;
    private final DriverService driverService;

    @Operation(summary = "View my deliveries")
    @GetMapping
    public List<Delivery> getMyDeliveries(
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return deliveryService.getDeliveriesForDriver(
                currentDriverId(currentUser)
        );
    }

    @Operation(summary = "Start my assigned delivery")
    @PatchMapping("/{deliveryId}/start")
    public Delivery startDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.startDelivery(
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Mark my delivery as delivered")
    @PatchMapping("/{deliveryId}/deliver")
    public Delivery markDelivered(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.markDelivered(
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Mark my delivery as failed")
    @PatchMapping("/{deliveryId}/fail")
    public Delivery markFailed(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.markFailed(
                currentDriverId(currentUser),
                deliveryId
        );
    }

    private Long currentDriverId(AppUser currentUser) {
        Driver driver = driverService.lambdaQuery()
                .eq(Driver::getUserId, currentUser.getId())
                .one();

        if (driver == null || !Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active driver account required"
            );
        }

        return driver.getId();
    }
}