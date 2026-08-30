package com.zachary.delivery_system.controller;


import com.zachary.delivery_system.dto.Delivery.AssignDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.CreateDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.UpdateDeliveryRequest;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Delivery management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    @Resource
    private DeliveryService deliveryService;

    @Operation(summary = "Create a delivery")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // When this method succeeds, return this HTTP status code.
    public Delivery createDelivery(@Valid @RequestBody CreateDeliveryRequest createDeliveryRequest) {
        Delivery delivery = new Delivery();
        delivery.setCustomerName(createDeliveryRequest.getCustomerName());
        delivery.setCustomerPhone(createDeliveryRequest.getCustomerPhone());
        delivery.setAddress(createDeliveryRequest.getAddress());
        applyDestinationCoordinates(
                delivery,
                createDeliveryRequest.getDestinationLatitude(),
                createDeliveryRequest.getDestinationLongitude()
        );
        delivery.setStatus("CREATED");

        deliveryService.save(delivery);
        return findDelivery(delivery.getId());
    }

    @Operation(summary = "List all deliveries")
    @GetMapping
    public List<Delivery> getDeliveries() {
        return deliveryService.list();
    }

    @Operation(summary = "Get a delivery by ID")
    @GetMapping("/{id}")
    public Delivery getDelivery(@PathVariable Long id) {
        return findDelivery(id);
    }

    public Delivery findDelivery(Long id) {
        Delivery delivery = deliveryService.getById(id);
        if (delivery == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Delivery not found: " + id
            );
        }
        return delivery;
    }

    @Operation(summary = "Update delivery customer details")
    @PostMapping("/{id}")
    public Delivery updateDelivery(@PathVariable Long id, @RequestBody @Valid UpdateDeliveryRequest updateDeliveryRequest) throws Exception {
        Delivery oldDelivery = deliveryService.getById(id);

        if (oldDelivery == null) throw new Exception("This delivery does not exist!");

        Delivery updatedDelivery = new Delivery();
        BeanUtils.copyProperties(oldDelivery, updatedDelivery);
        updatedDelivery.setCustomerName(updateDeliveryRequest.getCustomerName());
        updatedDelivery.setCustomerPhone(updateDeliveryRequest.getCustomerPhone());
        updatedDelivery.setAddress(updateDeliveryRequest.getAddress());
        applyDestinationCoordinates(
                updatedDelivery,
                updateDeliveryRequest.getDestinationLatitude(),
                updateDeliveryRequest.getDestinationLongitude()
        );
        deliveryService.updateById(updatedDelivery);
        return findDelivery(id);

    }

    @Operation(summary = "Delete a delivery")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDelivery(@PathVariable Long id) {
        Delivery delivery = findDelivery(id);
        deliveryService.removeById(delivery.getId());
    }

    @Operation(summary = "Assign a delivery to a driver")
    @PostMapping("/{id}/assignment")
    public Delivery assignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDeliveryRequest request
    ) {
        return deliveryService.assignDriver(id, request.getDriverId());
    }

    private void applyDestinationCoordinates(
            Delivery delivery,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude == null && longitude == null) {
            return;
        }

        if (latitude == null || longitude == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both destination latitude and longitude are required"
            );
        }

        delivery.setDestinationLatitude(latitude);
        delivery.setDestinationLongitude(longitude);
    }

}
