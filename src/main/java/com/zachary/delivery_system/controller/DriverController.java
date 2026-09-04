package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Driver.CreateDriverRequest;
import com.zachary.delivery_system.dto.Driver.UpdateDriverAvailabilityRequest;
import com.zachary.delivery_system.dto.Driver.UpdateDriverRequest;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DriverHasActiveDeliveriesException;
import com.zachary.delivery_system.exception.DriverNotFoundException;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    private final DeliveryService deliveryService;

    @Operation(summary = "Create a driver and login account")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Driver createDriver(@Valid @RequestBody CreateDriverRequest createDriverRequest) {
        return driverService.createDriver(createDriverRequest);
    }

    @Operation(summary = "List drivers")
    @GetMapping
    public List<Driver> getDrivers() {
        return driverService.list();
    }

    @Operation(summary = "Update a driver")
    @PutMapping("/{id}")
    public Driver updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        Driver driver = findDriver(id);

        driver.setFullName(request.getFullName());
        driver.setPhone(request.getPhone());
        if (request.getVehicleCapacityKg() != null) {
            driver.setVehicleCapacityKg(request.getVehicleCapacityKg());
        }
        if (request.getAvailable() != null) {
            driver.setAvailable(request.getAvailable());
        }

        driverService.updateById(driver);
        return driver;
    }

    @Operation(summary = "Change whether a driver is available for new work")
    @PatchMapping("/{id}/availability")
    public Driver updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverAvailabilityRequest request
    ) {
        Driver driver = findDriver(id);
        driver.setAvailable(request.getAvailable());
        driverService.updateById(driver);
        return driver;
    }

    @Operation(summary = "Activate a driver")
    @PatchMapping("/{id}/activate")
    public Driver activateDriver(@PathVariable Long id) {
        Driver driver = findDriver(id);

        driver.setActive(true);
        driverService.updateById(driver);

        return driver;
    }

    @Operation(summary = "Deactivate a driver (soft delete)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDriver(@PathVariable Long id) {
        Driver driver = findDriver(id);

        boolean hasActiveDeliveries = deliveryService.lambdaQuery()
                .eq(Delivery::getDriverId, id)
                .in(
                        Delivery::getStatus,
                        DeliveryStatus.driverWorkloadStatuses()
                )
                .exists();

        if (hasActiveDeliveries) {
            throw new DriverHasActiveDeliveriesException();
        }

        driver.setActive(false);
        driverService.updateById(driver);
    }

    @Operation(summary = "Get a driver by ID")
    @GetMapping("/{id}")
    public Driver getDriver(@PathVariable Long id) {
        return findDriver(id);
    }

    private Driver findDriver(Long id) {
        Driver driver = driverService.getById(id);

        if (driver == null) {
            throw new DriverNotFoundException(id);
        }

        return driver;
    }
}
