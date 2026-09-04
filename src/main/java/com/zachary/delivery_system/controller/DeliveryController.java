package com.zachary.delivery_system.controller;


import com.zachary.delivery_system.dto.Delivery.AssignDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.CreateDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.UpdateDeliveryRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.enums.DeliveryPriority;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryConcurrentUpdateException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.exception.InvalidDeliveryDestinationException;
import com.zachary.delivery_system.security.DeliveryAuthorizationService;
import com.zachary.delivery_system.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zachary.delivery_system.dto.PageResponse;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Delivery management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryAuthorizationService deliveryAuthorizationService;

    @Operation(summary = "Create a delivery")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // When this method succeeds, return this HTTP status code.
    public Delivery createDelivery(@Valid @RequestBody CreateDeliveryRequest createDeliveryRequest) {
        deliveryAuthorizationService.validateCustomerOwner(
                createDeliveryRequest.getOwnerId()
        );

        Delivery delivery = new Delivery();
        delivery.setOwnerId(createDeliveryRequest.getOwnerId());
        delivery.setCustomerName(createDeliveryRequest.getCustomerName());
        delivery.setCustomerPhone(createDeliveryRequest.getCustomerPhone());
        delivery.setAddress(createDeliveryRequest.getAddress());
        applyPickupCoordinates(
                delivery,
                createDeliveryRequest.getPickupLatitude(),
                createDeliveryRequest.getPickupLongitude()
        );
        applyDestinationCoordinates(
                delivery,
                createDeliveryRequest.getDestinationLatitude(),
                createDeliveryRequest.getDestinationLongitude()
        );
        delivery.setPriority(
                createDeliveryRequest.getPriority() == null
                        ? DeliveryPriority.NORMAL
                        : createDeliveryRequest.getPriority()
        );
        delivery.setPackageWeightKg(
                createDeliveryRequest.getPackageWeightKg() == null
                        ? BigDecimal.ONE
                        : createDeliveryRequest.getPackageWeightKg()
        );
        delivery.setTimeWindowStart(
                createDeliveryRequest.getTimeWindowStart()
        );
        delivery.setTimeWindowEnd(createDeliveryRequest.getTimeWindowEnd());
        delivery.setStatus(DeliveryStatus.CREATED);

        deliveryService.save(delivery);
        return findDelivery(delivery.getId());
    }

    @Operation(summary = "List deliveries with pagination")
    @GetMapping
    public PageResponse<Delivery> getDeliveries(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        Page<Delivery> pageRequest = Page.of(page, size);

        IPage<Delivery> result = deliveryService.page(
                pageRequest,
                deliveryAuthorizationService
                        .visibleDeliveryQuery(currentUser)
                        .orderByDesc(Delivery::getCreatedAt)
                        .orderByDesc(Delivery::getId)
        );

        return new PageResponse<>(
                result.getRecords(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages()
        );
    }

    @Operation(summary = "Get a delivery by ID")
    @GetMapping("/{id}")
    public Delivery getDelivery(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        Delivery delivery = findDelivery(id);
        deliveryAuthorizationService.assertCanView(currentUser, delivery);
        return delivery;
    }

    public Delivery findDelivery(Long id) {
        Delivery delivery = deliveryService.getById(id);
        if (delivery == null) {
            throw new DeliveryNotFoundException(id);
        }
        return delivery;
    }

    @Operation(summary = "Update delivery customer details")
    @PostMapping("/{id}")
    public Delivery updateDelivery(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDeliveryRequest updateDeliveryRequest
    ) {
        Delivery oldDelivery = findDelivery(id);

        Delivery updatedDelivery = new Delivery();
        BeanUtils.copyProperties(oldDelivery, updatedDelivery);
        updatedDelivery.setCustomerName(updateDeliveryRequest.getCustomerName());
        updatedDelivery.setCustomerPhone(updateDeliveryRequest.getCustomerPhone());
        updatedDelivery.setAddress(updateDeliveryRequest.getAddress());
        applyPickupCoordinates(
                updatedDelivery,
                updateDeliveryRequest.getPickupLatitude(),
                updateDeliveryRequest.getPickupLongitude()
        );
        applyDestinationCoordinates(
                updatedDelivery,
                updateDeliveryRequest.getDestinationLatitude(),
                updateDeliveryRequest.getDestinationLongitude()
        );
        if (updateDeliveryRequest.getPriority() != null) {
            updatedDelivery.setPriority(updateDeliveryRequest.getPriority());
        }
        if (updateDeliveryRequest.getPackageWeightKg() != null) {
            updatedDelivery.setPackageWeightKg(
                    updateDeliveryRequest.getPackageWeightKg()
            );
        }
        if (updateDeliveryRequest.getTimeWindowStart() != null) {
            updatedDelivery.setTimeWindowStart(
                    updateDeliveryRequest.getTimeWindowStart()
            );
            updatedDelivery.setTimeWindowEnd(
                    updateDeliveryRequest.getTimeWindowEnd()
            );
        }
        if (!deliveryService.updateById(updatedDelivery)) {
            throw new DeliveryConcurrentUpdateException();
        }
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
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AssignDeliveryRequest request
    ) {
        return deliveryService.assignDriver(
                currentUser,
                id,
                request.getDriverId()
        );
    }

    @Operation(summary = "Automatically assign the best eligible driver")
    @PostMapping("/{id}/auto-assignment")
    public Delivery autoAssignDriver(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long id
    ) {
        return deliveryService.autoAssignDriver(currentUser, id);
    }

    @Operation(summary = "Customer confirms a delivered delivery")
    @PatchMapping("/{id}/confirm")
    public Delivery confirmDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long id
    ) {
        return deliveryService.confirmDelivery(currentUser, id);
    }

    @Operation(summary = "Cancel a delivery")
    @PatchMapping("/{id}/cancel")
    public Delivery cancelDelivery(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long id
    ) {
        return deliveryService.cancelDelivery(currentUser, id);
    }

    @Operation(summary = "Mark a delivered or failed delivery as returned")
    @PatchMapping("/{id}/return")
    public Delivery markReturned(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable Long id
    ) {
        return deliveryService.markReturned(currentUser, id);
    }

    private void applyPickupCoordinates(
            Delivery delivery,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude == null && longitude == null) {
            return;
        }

        delivery.setPickupLatitude(latitude);
        delivery.setPickupLongitude(longitude);
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
            throw new InvalidDeliveryDestinationException();
        }

        delivery.setDestinationLatitude(latitude);
        delivery.setDestinationLongitude(longitude);
    }

}
