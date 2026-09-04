package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.enums.AuditAction;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryAlreadyAssignedException;
import com.zachary.delivery_system.exception.DeliveryAssignmentNotAllowedException;
import com.zachary.delivery_system.exception.DeliveryAccessDeniedException;
import com.zachary.delivery_system.exception.DeliveryConcurrentUpdateException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.exception.DriverInactiveException;
import com.zachary.delivery_system.exception.DriverConcurrentAssignmentException;
import com.zachary.delivery_system.exception.DriverNotFoundException;
import com.zachary.delivery_system.service.DriverService;
import com.zachary.delivery_system.service.assignment.DriverAssignmentDecision;
import com.zachary.delivery_system.service.assignment.DriverAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DriverService driverService;

    @Mock
    private AuditService auditService;

    @Mock
    private DriverAssignmentService driverAssignmentService;

    @Test
    void assignDriver_assignsAnActiveDriverToACreatedDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        Driver driver = activeDriver(8L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(driver);
        when(driverService.updateById(driver)).thenReturn(true);

        Delivery result = service.assignDriver(actor, 5L, 8L);

        assertEquals(8L, result.getDriverId());
        assertEquals(DeliveryStatus.ASSIGNED, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        // check if service do call updateById
        verify(service).updateById(delivery);
        verifyAudit(
                actor,
                AuditAction.DELIVERY_DRIVER_ASSIGNED,
                DeliveryStatus.CREATED,
                DeliveryStatus.ASSIGNED
        );
    }

    @Test
    void assignDriver_rejectsADeliveryThatIsNotCreated() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        DeliveryAssignmentNotAllowedException exception = assertThrows(
                DeliveryAssignmentNotAllowedException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DELIVERY_ASSIGNMENT_NOT_ALLOWED", exception.getCode());
        assertEquals("Only a CREATED delivery can be assigned", exception.getMessage());
        verifyNoInteractions(driverService);
    }

    @Test
    void assignDriver_returnsNotFoundWhenDeliveryDoesNotExist() {
        DeliveryServiceImpl service =
                spy(new DeliveryServiceImpl(
                        driverService,
                        auditService,
                        driverAssignmentService
                ));
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");

        doReturn(null).when(service).getById(5L);

        DeliveryNotFoundException exception = assertThrows(
                DeliveryNotFoundException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DELIVERY_NOT_FOUND", exception.getCode());
        assertEquals("Delivery not found: 5", exception.getMessage());
        verifyNoInteractions(driverService);
    }

    @Test
    void assignDriver_returnsNotFoundWhenDriverDoesNotExist() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(null);

        DriverNotFoundException exception = assertThrows(
                DriverNotFoundException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DRIVER_NOT_FOUND", exception.getCode());
        assertEquals("Driver not found: 8", exception.getMessage());
    }

    @Test
    void assignDriver_returnsConflictWhenDriverIsInactive() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        Driver inactiveDriver = activeDriver(8L);
        inactiveDriver.setActive(false);

        when(driverService.getById(8L)).thenReturn(inactiveDriver);

        DriverInactiveException exception = assertThrows(
                DriverInactiveException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DRIVER_INACTIVE", exception.getCode());
        assertEquals(
                "Cannot assign a delivery to an inactive driver",
                exception.getMessage()
        );
    }

    @Test
    void assignDriver_returnsConflictWhenDeliveryIsAlreadyAssigned() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.ASSIGNED);
        delivery.setDriverId(1L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");

        DeliveryServiceImpl service = serviceReturning(delivery);

        DeliveryAlreadyAssignedException exception = assertThrows(
                DeliveryAlreadyAssignedException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DELIVERY_ALREADY_ASSIGNED", exception.getCode());
        assertEquals(
                "Delivery has already been assigned",
                exception.getMessage()
        );

        verifyNoInteractions(driverService);
    }

    @Test
    void assignDriver_returnsConflictWhenAnotherDispatcherUpdatesFirst() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        delivery.setVersion(0L);
        Driver driver = activeDriver(8L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");

        DeliveryServiceImpl service =
                spy(new DeliveryServiceImpl(
                        driverService,
                        auditService,
                        driverAssignmentService
                ));

        doReturn(delivery).when(service).getById(5L);
        doReturn(false).when(service).updateById(delivery);

        when(driverService.getById(8L)).thenReturn(driver);
        when(driverService.updateById(driver)).thenReturn(true);

        DeliveryConcurrentUpdateException exception = assertThrows(
                DeliveryConcurrentUpdateException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DELIVERY_CONCURRENT_UPDATE", exception.getCode());
        assertEquals(
                "This delivery was changed by another request. Please refresh and try again.",
                exception.getMessage()
        );
        verifyNoInteractions(auditService);
    }

    @Test
    void assignDriver_returnsConflictWhenDriverWorkloadChangesConcurrently() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        Driver driver = activeDriver(8L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(driver);
        when(driverService.updateById(driver)).thenReturn(false);

        DriverConcurrentAssignmentException exception = assertThrows(
                DriverConcurrentAssignmentException.class,
                () -> service.assignDriver(actor, 5L, 8L)
        );

        assertEquals("DRIVER_CONCURRENT_ASSIGNMENT", exception.getCode());
        verifyNoInteractions(auditService);
    }

    @Test
    void startDelivery_recordsWhoStartedIt() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.PICKED_UP);
        delivery.setDriverId(8L);
        AppUser actor = actor(3L, "driver1", "DRIVER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        Delivery result = service.startDelivery(actor, 8L, 5L);

        assertEquals(DeliveryStatus.IN_TRANSIT, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(service, times(1)).updateById(delivery);
        verifyAudit(
                actor,
                AuditAction.DELIVERY_STARTED,
                DeliveryStatus.PICKED_UP,
                DeliveryStatus.IN_TRANSIT
        );
    }

    @Test
    void acceptAndPickup_followTheLifecycleAndCreateAuditEvents() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.ASSIGNED);
        delivery.setDriverId(8L);
        AppUser actor = actor(3L, "driver1", "DRIVER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        service.acceptDelivery(actor, 8L, 5L);
        assertEquals(DeliveryStatus.ACCEPTED, delivery.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_ACCEPTED,
                DeliveryStatus.ASSIGNED,
                DeliveryStatus.ACCEPTED
        );

        service.pickupDelivery(actor, 8L, 5L);
        assertEquals(DeliveryStatus.PICKED_UP, delivery.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_PICKED_UP,
                DeliveryStatus.ACCEPTED,
                DeliveryStatus.PICKED_UP
        );
        verify(service, times(2)).updateById(delivery);
    }

    @Test
    void markDelivered_changesAnInTransitDeliveryAndSetsDeliveredTime() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setDriverId(8L);
        AppUser actor = actor(3L, "driver1", "DRIVER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        Delivery result = service.markDelivered(actor, 8L, 5L);

        assertEquals(DeliveryStatus.DELIVERED, result.getStatus());
        assertNotNull(result.getDeliveredAt());
        assertNotNull(result.getUpdatedAt());
        verify(service, times(1)).updateById(delivery);
        verifyAudit(
                actor,
                AuditAction.DELIVERY_DELIVERED,
                DeliveryStatus.IN_TRANSIT,
                DeliveryStatus.DELIVERED
        );
    }

    @Test
    void markFailed_recordsWhoFailedIt() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setDriverId(8L);
        AppUser actor = actor(3L, "driver1", "DRIVER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        Delivery result = service.markFailed(actor, 8L, 5L);

        assertEquals(DeliveryStatus.FAILED, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(service, times(1)).updateById(delivery);
        verifyAudit(
                actor,
                AuditAction.DELIVERY_FAILED,
                DeliveryStatus.IN_TRANSIT,
                DeliveryStatus.FAILED
        );
    }

    @Test
    void retryAndResume_moveAFailedDeliveryBackIntoTransit() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.FAILED);
        delivery.setDriverId(8L);
        AppUser actor = actor(3L, "driver1", "DRIVER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        service.retryDelivery(actor, 8L, 5L);
        assertEquals(DeliveryStatus.RETRY, delivery.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_RETRY_REQUESTED,
                DeliveryStatus.FAILED,
                DeliveryStatus.RETRY
        );

        service.resumeDelivery(actor, 8L, 5L);
        assertEquals(DeliveryStatus.IN_TRANSIT, delivery.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_TRANSIT_RESUMED,
                DeliveryStatus.RETRY,
                DeliveryStatus.IN_TRANSIT
        );
    }

    @Test
    void confirmDelivery_allowsTheOwningCustomerToCompleteDelivery() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        delivery.setDriverId(8L);
        delivery.setOwnerId(12L);
        AppUser actor = actor(12L, "customer1", "CUSTOMER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        Delivery result = service.confirmDelivery(actor, 5L);

        assertEquals(DeliveryStatus.COMPLETED, result.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_CUSTOMER_CONFIRMED,
                DeliveryStatus.DELIVERED,
                DeliveryStatus.COMPLETED
        );
    }

    @Test
    void confirmDelivery_rejectsAnotherCustomer() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        delivery.setOwnerId(12L);
        AppUser actor = actor(13L, "customer2", "CUSTOMER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        assertThrows(
                DeliveryAccessDeniedException.class,
                () -> service.confirmDelivery(actor, 5L)
        );
        verifyNoInteractions(auditService);
    }

    @Test
    void cancelDelivery_movesAnUnassignedDeliveryToCancelled() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        Delivery result = service.cancelDelivery(actor, 5L);

        assertEquals(DeliveryStatus.CANCELLED, result.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_CANCELLED,
                DeliveryStatus.CREATED,
                DeliveryStatus.CANCELLED,
                null
        );
    }

    @Test
    void markReturned_movesADeliveredDeliveryToReturned() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        delivery.setDriverId(8L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        Delivery result = service.markReturned(actor, 5L);

        assertEquals(DeliveryStatus.RETURNED, result.getStatus());
        verifyAudit(
                actor,
                AuditAction.DELIVERY_RETURNED,
                DeliveryStatus.DELIVERED,
                DeliveryStatus.RETURNED
        );
    }

    @Test
    void autoAssignDriver_usesTheSingleDriverChosenByTheAlgorithm() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.CREATED);
        Driver driver = activeDriver(8L);
        AppUser actor = actor(2L, "dispatcher", "DISPATCHER");
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverAssignmentService.selectBestDriver(delivery))
                .thenReturn(new DriverAssignmentDecision(
                        driver,
                        7.5,
                        2.5,
                        1,
                        new java.math.BigDecimal("4.00")
                ));
        when(driverService.updateById(driver)).thenReturn(true);

        Delivery result = service.autoAssignDriver(actor, 5L);

        assertEquals(8L, result.getDriverId());
        assertEquals(DeliveryStatus.ASSIGNED, result.getStatus());
        verify(driverAssignmentService).selectBestDriver(delivery);
        verifyAudit(
                actor,
                AuditAction.DELIVERY_DRIVER_ASSIGNED,
                DeliveryStatus.CREATED,
                DeliveryStatus.ASSIGNED
        );
    }

    @Test
    void assignDriverRejectsANullDeliveryStatus() {
        Delivery delivery = deliveryWithStatus(null);
        DeliveryServiceImpl service = serviceReturning(delivery);

        assertThrows(
                DeliveryAssignmentNotAllowedException.class,
                () -> service.assignDriver(
                        actor(2L, "dispatcher", "DISPATCHER"),
                        5L,
                        8L
                )
        );

        verifyNoInteractions(driverService, auditService);
    }

    @Test
    void lifecycleRejectsNullAndInvalidPreviousStatuses() {
        Delivery nullStatus = deliveryWithStatus(null);
        nullStatus.setDriverId(8L);
        DeliveryServiceImpl nullStatusService = serviceReturning(nullStatus);
        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        assertThrows(
                com.zachary.delivery_system.exception
                        .DeliveryStatusTransitionException.class,
                () -> nullStatusService.startDelivery(
                        actor(3L, "driver", "DRIVER"),
                        8L,
                        5L
                )
        );

        Delivery invalidStatus = deliveryWithStatus(DeliveryStatus.ASSIGNED);
        invalidStatus.setDriverId(8L);
        DeliveryServiceImpl invalidStatusService =
                serviceReturning(invalidStatus);

        assertThrows(
                com.zachary.delivery_system.exception
                        .DeliveryStatusTransitionException.class,
                () -> invalidStatusService.startDelivery(
                        actor(3L, "driver", "DRIVER"),
                        8L,
                        5L
                )
        );

        verifyNoInteractions(auditService);
    }

    @Test
    void driverWorkflowRejectsUnknownAndUnassignedDrivers() {
        DeliveryServiceImpl missingDriverService = new DeliveryServiceImpl(
                driverService,
                auditService,
                driverAssignmentService
        );
        when(driverService.getById(8L)).thenReturn(null);

        assertThrows(
                DriverNotFoundException.class,
                () -> missingDriverService.startDelivery(
                        actor(3L, "driver", "DRIVER"),
                        8L,
                        5L
                )
        );

        Delivery assignedToAnotherDriver =
                deliveryWithStatus(DeliveryStatus.PICKED_UP);
        assignedToAnotherDriver.setDriverId(9L);
        DeliveryServiceImpl wrongDriverService =
                serviceReturning(assignedToAnotherDriver);
        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        assertThrows(
                DeliveryAccessDeniedException.class,
                () -> wrongDriverService.startDelivery(
                        actor(3L, "driver", "DRIVER"),
                        8L,
                        5L
                )
        );

        verifyNoInteractions(auditService);
    }

    @Test
    void confirmDeliveryRejectsMissingActor() {
        Delivery delivery = deliveryWithStatus(DeliveryStatus.DELIVERED);
        delivery.setOwnerId(12L);
        DeliveryServiceImpl service = serviceReturning(delivery);

        assertThrows(
                DeliveryAccessDeniedException.class,
                () -> service.confirmDelivery(null, 5L)
        );

        verifyNoInteractions(auditService);
    }

    private DeliveryServiceImpl serviceReturning(Delivery delivery) {
        DeliveryServiceImpl service = spy(
                new DeliveryServiceImpl(
                        driverService,
                        auditService,
                        driverAssignmentService
                )
        );
        doReturn(delivery).when(service).getById(5L);
        lenient().doReturn(true)
                .when(service).updateById(any(Delivery.class));
        return service;
    }

    private void verifyAudit(
            AppUser actor,
            AuditAction action,
            DeliveryStatus previousStatus,
            DeliveryStatus newStatus
    ) {
        verifyAudit(
                actor,
                action,
                previousStatus,
                newStatus,
                8L
        );
    }

    private void verifyAudit(
            AppUser actor,
            AuditAction action,
            DeliveryStatus previousStatus,
            DeliveryStatus newStatus,
            Long expectedDriverId
    ) {
        verify(auditService).record(
                eq(actor),
                eq(action),
                eq("DELIVERY"),
                eq(5L),
                argThat(details ->
                        (expectedDriverId == null
                                ? !details.containsKey("driverId")
                                : expectedDriverId.equals(
                                        details.get("driverId")
                                ))
                                && previousStatus.name().equals(
                                details.get("previousStatus")
                        )
                                && newStatus.name().equals(
                                details.get("newStatus")
                        )
                )
        );
    }

    private Delivery deliveryWithStatus(DeliveryStatus status) {
        Delivery delivery = new Delivery();
        delivery.setId(5L);
        delivery.setStatus(status);
        return delivery;
    }

    private Driver activeDriver(Long driverId) {
        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setActive(true);
        driver.setVersion(0L);
        return driver;
    }

    private AppUser actor(Long id, String username, String role) {
        AppUser actor = new AppUser();
        actor.setId(id);
        actor.setUsername(username);
        actor.setRole(role);
        return actor;
    }
}
