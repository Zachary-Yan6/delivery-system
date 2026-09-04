package com.zachary.delivery_system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.DeliveryAccessDeniedException;
import com.zachary.delivery_system.exception.DeliveryOwnerInvalidException;
import com.zachary.delivery_system.service.AppUserService;
import com.zachary.delivery_system.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Contains the policy for access to one Delivery. Route-level security answers
 * "may this role call this endpoint?"; this service answers "may this user
 * access this particular Delivery?".
 */
@Service
@RequiredArgsConstructor
public class DeliveryAuthorizationService {

    private final DriverService driverService;
    private final AppUserService appUserService;

    public void assertCanView(AppUser currentUser, Delivery delivery) {
        if (isDispatcherOrAdmin(currentUser)) {
            return;
        }

        if ("CUSTOMER".equals(currentUser.getRole())
                && Objects.equals(delivery.getOwnerId(), currentUser.getId())) {
            return;
        }

        if ("DRIVER".equals(currentUser.getRole())
                && Objects.equals(delivery.getDriverId(), activeDriverId(currentUser))) {
            return;
        }

        throw new DeliveryAccessDeniedException();
    }

    /**
     * Produces a database-side filter so a customer or driver never receives a
     * complete delivery list and relies on the frontend to hide it.
     */
    public LambdaQueryWrapper<Delivery> visibleDeliveryQuery(AppUser currentUser) {
        LambdaQueryWrapper<Delivery> query = new LambdaQueryWrapper<>();

        if (isDispatcherOrAdmin(currentUser)) {
            return query;
        }

        if ("CUSTOMER".equals(currentUser.getRole())) {
            return query.eq(Delivery::getOwnerId, currentUser.getId());
        }

        if ("DRIVER".equals(currentUser.getRole())) {
            Long driverId = activeDriverId(currentUser);
            if (driverId != null) {
                return query.eq(Delivery::getDriverId, driverId);
            }
        }

        throw new DeliveryAccessDeniedException();
    }

    /**
     * Dispatcher-created deliveries may name a customer owner. Null remains
     * allowed only for legacy deliveries while existing data is migrated.
     */
    public void validateCustomerOwner(Long ownerId) {
        if (ownerId == null) {
            return;
        }

        AppUser owner = appUserService.getById(ownerId);
        if (owner == null || !"CUSTOMER".equals(owner.getRole())) {
            throw new DeliveryOwnerInvalidException();
        }
    }

    private boolean isDispatcherOrAdmin(AppUser currentUser) {
        return "DISPATCHER".equals(currentUser.getRole())
                || "ADMIN".equals(currentUser.getRole());
    }

    private Long activeDriverId(AppUser currentUser) {
        Driver driver = driverService.lambdaQuery()
                .eq(Driver::getUserId, currentUser.getId())
                .eq(Driver::getActive, true)
                .one();

        return driver == null ? null : driver.getId();
    }
}
