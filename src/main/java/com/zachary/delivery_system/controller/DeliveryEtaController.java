package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Delivery.DeliveryEtaResponse;
import com.zachary.delivery_system.service.DeliveryEtaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatcher/deliveries")
@RequiredArgsConstructor
@Tag(name = "Delivery ETA", description = "Live delivery ETA APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryEtaController {

    private final DeliveryEtaService deliveryEtaService;

    @Operation(summary = "Calculate ETA using fixed driver speed")
    @GetMapping("/{deliveryId}/eta")
    public DeliveryEtaResponse calculateEta(
            @PathVariable Long deliveryId
    ) {
        return deliveryEtaService.calculateEta(deliveryId);
    }
}