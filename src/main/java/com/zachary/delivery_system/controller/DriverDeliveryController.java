package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.DriverAccountUnavailableException;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Accept my assigned delivery")
    @PatchMapping("/{deliveryId}/accept")
    public Delivery acceptDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.acceptDelivery(
                currentUser,
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Confirm that I picked up the delivery")
    @PatchMapping("/{deliveryId}/pickup")
    public Delivery pickupDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.pickupDelivery(
                currentUser,
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Start transit after pickup")
    @PatchMapping("/{deliveryId}/start")
    public Delivery startDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.startDelivery(
                currentUser,
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
                currentUser,
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
                currentUser,
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Request a retry for my failed delivery")
    @PatchMapping("/{deliveryId}/retry")
    public Delivery retryDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.retryDelivery(
                currentUser,
                currentDriverId(currentUser),
                deliveryId
        );
    }

    @Operation(summary = "Resume transit after a retry")
    @PatchMapping("/{deliveryId}/resume")
    public Delivery resumeDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long deliveryId
    ) {
        return deliveryService.resumeDelivery(
                currentUser,
                currentDriverId(currentUser),
                deliveryId
        );
    }

    private Long currentDriverId(AppUser currentUser) {
        Driver driver = driverService.lambdaQuery()
                .eq(Driver::getUserId, currentUser.getId())
                .one();

        if (driver == null || !Boolean.TRUE.equals(driver.getActive())) {
            throw new DriverAccountUnavailableException();
        }

        return driver.getId();
    }
}
