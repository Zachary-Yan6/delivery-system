package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.enums.AuditAction;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryAccessDeniedException;
import com.zachary.delivery_system.exception.DeliveryAlreadyAssignedException;
import com.zachary.delivery_system.exception.DeliveryAssignmentNotAllowedException;
import com.zachary.delivery_system.exception.DeliveryConcurrentUpdateException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.exception.DeliveryStatusTransitionException;
import com.zachary.delivery_system.exception.DriverInactiveException;
import com.zachary.delivery_system.exception.DriverConcurrentAssignmentException;
import com.zachary.delivery_system.exception.DriverNotFoundException;
import com.zachary.delivery_system.mapper.DeliveryMapper;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import com.zachary.delivery_system.service.assignment.DriverAssignmentDecision;
import com.zachary.delivery_system.service.assignment.DriverAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl extends ServiceImpl<DeliveryMapper, Delivery>
        implements DeliveryService {

    private final DriverService driverService;
    private final AuditService auditService;
    private final DriverAssignmentService driverAssignmentService;

    @Override
    public Delivery assignDriver(
            AppUser actor,
            Long deliveryId,
            Long driverId
    ) {
        Delivery delivery = requireAssignableDelivery(deliveryId);
        Driver driver = requireActiveDriver(driverId);

        return assign(
                actor,
                delivery,
                driver,
                Map.of("assignmentMode", "MANUAL")
        );
    }

    @Override
    public Delivery autoAssignDriver(AppUser actor, Long deliveryId) {
        Delivery delivery = requireAssignableDelivery(deliveryId);
        DriverAssignmentDecision decision =
                driverAssignmentService.selectBestDriver(delivery);

        return assign(
                actor,
                delivery,
                decision.driver(),
                Map.of(
                        "assignmentMode", "AUTOMATIC",
                        "score", decision.score(),
                        "distanceKm", decision.distanceKm(),
                        "activeDeliveryCount",
                        decision.activeDeliveryCount(),
                        "activeLoadKg", decision.activeLoadKg()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Delivery> getDeliveriesForDriver(Long driverId) {
        validateDriverExists(driverId);

        QueryWrapper<Delivery> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("driver_id", driverId);

        return list(queryWrapper);
    }

    @Override
    public Delivery acceptDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.ACCEPTED,
                AuditAction.DELIVERY_ACCEPTED
        );
    }

    @Override
    public Delivery pickupDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.PICKED_UP,
                AuditAction.DELIVERY_PICKED_UP
        );
    }

    @Override
    public Delivery startDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.IN_TRANSIT,
                AuditAction.DELIVERY_STARTED
        );
    }

    @Override
    public Delivery markDelivered(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.DELIVERED,
                AuditAction.DELIVERY_DELIVERED
        );
    }

    @Override
    public Delivery markFailed(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.FAILED,
                AuditAction.DELIVERY_FAILED
        );
    }

    @Override
    public Delivery retryDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.RETRY,
                AuditAction.DELIVERY_RETRY_REQUESTED
        );
    }

    @Override
    public Delivery resumeDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    ) {
        return changeDriverDeliveryStatus(
                actor,
                driverId,
                deliveryId,
                DeliveryStatus.IN_TRANSIT,
                AuditAction.DELIVERY_TRANSIT_RESUMED
        );
    }

    @Override
    public Delivery confirmDelivery(AppUser actor, Long deliveryId) {
        Delivery delivery = requireDelivery(deliveryId);
        assertCanConfirm(actor, delivery);

        return changeDeliveryStatus(
                actor,
                delivery,
                DeliveryStatus.COMPLETED,
                AuditAction.DELIVERY_CUSTOMER_CONFIRMED,
                Map.of()
        );
    }

    @Override
    public Delivery cancelDelivery(AppUser actor, Long deliveryId) {
        Delivery delivery = requireDelivery(deliveryId);

        return changeDeliveryStatus(
                actor,
                delivery,
                DeliveryStatus.CANCELLED,
                AuditAction.DELIVERY_CANCELLED,
                Map.of()
        );
    }

    @Override
    public Delivery markReturned(AppUser actor, Long deliveryId) {
        Delivery delivery = requireDelivery(deliveryId);

        return changeDeliveryStatus(
                actor,
                delivery,
                DeliveryStatus.RETURNED,
                AuditAction.DELIVERY_RETURNED,
                Map.of()
        );
    }

    private Delivery assign(
            AppUser actor,
            Delivery delivery,
            Driver driver,
            Map<String, Object> assignmentDetails
    ) {
        reserveDriverForAssignment(driver);
        delivery.setDriverId(driver.getId());

        return changeDeliveryStatus(
                actor,
                delivery,
                DeliveryStatus.ASSIGNED,
                AuditAction.DELIVERY_DRIVER_ASSIGNED,
                assignmentDetails
        );
    }

    private Delivery changeDriverDeliveryStatus(
            AppUser actor,
            Long driverId,
            Long deliveryId,
            DeliveryStatus newStatus,
            AuditAction auditAction
    ) {
        Delivery delivery = getDriverDelivery(driverId, deliveryId);

        return changeDeliveryStatus(
                actor,
                delivery,
                newStatus,
                auditAction,
                Map.of()
        );
    }

    private Delivery changeDeliveryStatus(
            AppUser actor,
            Delivery delivery,
            DeliveryStatus newStatus,
            AuditAction auditAction,
            Map<String, Object> additionalAuditDetails
    ) {
        DeliveryStatus previousStatus = delivery.getStatus();

        if (previousStatus == null
                || !previousStatus.canTransitionTo(newStatus)) {
            throw new DeliveryStatusTransitionException(
                    previousStatus,
                    newStatus
            );
        }

        Date now = new Date();
        delivery.setStatus(newStatus);
        delivery.setUpdatedAt(now);

        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(now);
        }

        updateOrThrowConflict(delivery);
        recordDeliveryAudit(
                actor,
                auditAction,
                delivery,
                previousStatus,
                additionalAuditDetails
        );

        return delivery;
    }

    private Delivery requireAssignableDelivery(Long deliveryId) {
        Delivery delivery = requireDelivery(deliveryId);

        if (delivery.getDriverId() != null) {
            throw new DeliveryAlreadyAssignedException();
        }

        if (delivery.getStatus() == null
                || !delivery.getStatus().canAssign()) {
            throw new DeliveryAssignmentNotAllowedException();
        }

        return delivery;
    }

    private Delivery requireDelivery(Long deliveryId) {
        Delivery delivery = getById(deliveryId);

        if (delivery == null) {
            throw new DeliveryNotFoundException(deliveryId);
        }

        return delivery;
    }

    private Driver requireActiveDriver(Long driverId) {
        Driver driver = driverService.getById(driverId);

        if (driver == null) {
            throw new DriverNotFoundException(driverId);
        }

        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new DriverInactiveException();
        }

        return driver;
    }

    private void reserveDriverForAssignment(Driver driver) {
        if (!driverService.updateById(driver)) {
            throw new DriverConcurrentAssignmentException();
        }
    }

    private void validateDriverExists(Long driverId) {
        if (driverService.getById(driverId) == null) {
            throw new DriverNotFoundException(driverId);
        }
    }

    private Delivery getDriverDelivery(Long driverId, Long deliveryId) {
        validateDriverExists(driverId);
        Delivery delivery = requireDelivery(deliveryId);

        if (!driverId.equals(delivery.getDriverId())) {
            throw new DeliveryAccessDeniedException();
        }

        return delivery;
    }

    private void assertCanConfirm(AppUser actor, Delivery delivery) {
        if (actor == null) {
            throw new DeliveryAccessDeniedException();
        }

        if ("ADMIN".equals(actor.getRole())
                || "DISPATCHER".equals(actor.getRole())) {
            return;
        }

        if ("CUSTOMER".equals(actor.getRole())
                && Objects.equals(actor.getId(), delivery.getOwnerId())) {
            return;
        }

        throw new DeliveryAccessDeniedException();
    }

    private void recordDeliveryAudit(
            AppUser actor,
            AuditAction action,
            Delivery delivery,
            DeliveryStatus previousStatus,
            Map<String, Object> additionalDetails
    ) {
        Map<String, Object> details = new LinkedHashMap<>();

        if (delivery.getDriverId() != null) {
            details.put("driverId", delivery.getDriverId());
        }

        details.put("previousStatus", previousStatus.name());
        details.put("newStatus", delivery.getStatus().name());
        details.putAll(additionalDetails);

        auditService.record(
                actor,
                action,
                "DELIVERY",
                delivery.getId(),
                details
        );
    }

    private void updateOrThrowConflict(Delivery delivery) {
        if (!updateById(delivery)) {
            throw new DeliveryConcurrentUpdateException();
        }
    }
}
