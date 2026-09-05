package com.zachary.delivery_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zachary.delivery_system.dto.Delivery.AssignDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.CreateDeliveryRequest;
import com.zachary.delivery_system.dto.Delivery.CreateDeliveryResponse;
import com.zachary.delivery_system.dto.Delivery.UpdateDeliveryRequest;
import com.zachary.delivery_system.dto.PageResponse;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.enums.DeliveryPriority;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryConcurrentUpdateException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.exception.InvalidDeliveryDestinationException;
import com.zachary.delivery_system.security.DeliveryAuthorizationService;
import com.zachary.delivery_system.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;
    @Mock
    private DeliveryAuthorizationService authorizationService;

    private DeliveryController controller;

    @BeforeEach
    void setUp() {
        controller = new DeliveryController(
                deliveryService,
                authorizationService
        );
    }

    @Test
    void createDeliveryAppliesDefaultsAndReturnsACreationResponse() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setOwnerId(12L);
        request.setCustomerName("Customer");
        request.setCustomerPhone("123");
        request.setAddress("London");
        request.setPriority(null);
        request.setPackageWeightKg(null);
        AtomicReference<Delivery> saved = captureSavedDelivery();

        CreateDeliveryResponse result = controller.createDelivery(request);

        assertEquals(5L, result.id());
        assertEquals(DeliveryStatus.CREATED, result.status());
        assertEquals(saved.get().getCreatedAt().toInstant(), result.createdAt());
        assertEquals(12L, saved.get().getOwnerId());
        assertEquals("Customer", saved.get().getCustomerName());
        assertEquals("123", saved.get().getCustomerPhone());
        assertEquals("London", saved.get().getAddress());
        assertEquals(DeliveryPriority.NORMAL, saved.get().getPriority());
        assertEquals(BigDecimal.ONE, saved.get().getPackageWeightKg());
        assertNull(saved.get().getPickupLatitude());
        assertNull(saved.get().getDestinationLatitude());
        verify(authorizationService).validateCustomerOwner(12L);
    }

    @Test
    void createDeliveryCopiesCoordinatesPriorityWeightAndTimeWindow() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setCustomerName("Customer");
        request.setAddress("Address");
        request.setPickupLatitude(new BigDecimal("51.50"));
        request.setPickupLongitude(new BigDecimal("-0.12"));
        request.setDestinationLatitude(new BigDecimal("51.60"));
        request.setDestinationLongitude(new BigDecimal("-0.13"));
        request.setPriority(DeliveryPriority.HIGH);
        request.setPackageWeightKg(new BigDecimal("4.50"));
        Date start = new Date(System.currentTimeMillis() + 60_000);
        Date end = new Date(start.getTime() + 60_000);
        request.setTimeWindowStart(start);
        request.setTimeWindowEnd(end);
        AtomicReference<Delivery> saved = captureSavedDelivery();

        CreateDeliveryResponse result = controller.createDelivery(request);

        assertEquals(5L, result.id());
        assertEquals(DeliveryStatus.CREATED, result.status());
        assertEquals(request.getPickupLatitude(), saved.get().getPickupLatitude());
        assertEquals(request.getPickupLongitude(), saved.get().getPickupLongitude());
        assertEquals(
                request.getDestinationLatitude(),
                saved.get().getDestinationLatitude()
        );
        assertEquals(
                request.getDestinationLongitude(),
                saved.get().getDestinationLongitude()
        );
        assertEquals(DeliveryPriority.HIGH, saved.get().getPriority());
        assertEquals(new BigDecimal("4.50"), saved.get().getPackageWeightKg());
        assertSame(start, saved.get().getTimeWindowStart());
        assertSame(end, saved.get().getTimeWindowEnd());
    }

    @Test
    void createDeliveryRejectsAPartialDestination() {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setDestinationLatitude(new BigDecimal("51.50"));

        assertThrows(
                InvalidDeliveryDestinationException.class,
                () -> controller.createDelivery(request)
        );

        verify(deliveryService, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getDeliveriesReturnsPageMetadataAndRecords() {
        AppUser currentUser = new AppUser();
        LambdaQueryWrapper<Delivery> query = mockQuery();
        Page<Delivery> databasePage = new Page<>(2, 5, 11);
        Delivery delivery = delivery(7L);
        databasePage.setRecords(List.of(delivery));
        when(authorizationService.visibleDeliveryQuery(currentUser))
                .thenReturn(query);
        when(deliveryService.page(any(Page.class), same(query)))
                .thenReturn(databasePage);

        PageResponse<Delivery> response =
                controller.getDeliveries(currentUser, 2, 5);

        assertEquals(List.of(delivery), response.content());
        assertEquals(2, response.page());
        assertEquals(5, response.size());
        assertEquals(11, response.totalElements());
        assertEquals(3, response.totalPages());
    }

    @Test
    void getDeliveryAuthorizesAndReturnsAnExistingDelivery() {
        Delivery delivery = delivery(5L);
        AppUser user = new AppUser();
        when(deliveryService.getById(5L)).thenReturn(delivery);

        Delivery result = controller.getDelivery(5L, user);

        assertSame(delivery, result);
        verify(authorizationService).assertCanView(user, delivery);
    }

    @Test
    void findDeliveryRejectsAnUnknownId() {
        when(deliveryService.getById(99L)).thenReturn(null);

        assertThrows(
                DeliveryNotFoundException.class,
                () -> controller.findDelivery(99L)
        );
    }

    @Test
    void updateDeliveryCopiesEveryProvidedFieldAndReloadsTheResult() {
        Delivery oldDelivery = delivery(5L);
        oldDelivery.setPriority(DeliveryPriority.NORMAL);
        oldDelivery.setPackageWeightKg(BigDecimal.ONE);
        Delivery reloaded = delivery(5L);
        when(deliveryService.getById(5L)).thenReturn(oldDelivery, reloaded);
        when(deliveryService.updateById(any(Delivery.class))).thenReturn(true);
        UpdateDeliveryRequest request = completeUpdateRequest();

        Delivery result = controller.updateDelivery(5L, request);

        assertSame(reloaded, result);
        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryService).updateById(captor.capture());
        Delivery update = captor.getValue();
        assertEquals("Updated", update.getCustomerName());
        assertEquals("456", update.getCustomerPhone());
        assertEquals("Manchester", update.getAddress());
        assertEquals(request.getPickupLatitude(), update.getPickupLatitude());
        assertEquals(request.getDestinationLongitude(), update.getDestinationLongitude());
        assertEquals(DeliveryPriority.URGENT, update.getPriority());
        assertEquals(new BigDecimal("7.00"), update.getPackageWeightKg());
        assertSame(request.getTimeWindowStart(), update.getTimeWindowStart());
        assertSame(request.getTimeWindowEnd(), update.getTimeWindowEnd());
    }

    @Test
    void updateDeliveryKeepsOptionalFieldsWhenOmitted() {
        Delivery oldDelivery = delivery(5L);
        oldDelivery.setPriority(DeliveryPriority.HIGH);
        oldDelivery.setPackageWeightKg(new BigDecimal("3.00"));
        when(deliveryService.getById(5L)).thenReturn(oldDelivery, oldDelivery);
        when(deliveryService.updateById(any(Delivery.class))).thenReturn(true);
        UpdateDeliveryRequest request = new UpdateDeliveryRequest();

        controller.updateDelivery(5L, request);

        ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryService).updateById(captor.capture());
        assertEquals(DeliveryPriority.HIGH, captor.getValue().getPriority());
        assertEquals(new BigDecimal("3.00"), captor.getValue().getPackageWeightKg());
        assertNull(captor.getValue().getTimeWindowStart());
    }

    @Test
    void updateDeliveryReportsAnOptimisticLockConflict() {
        when(deliveryService.getById(5L)).thenReturn(delivery(5L));
        when(deliveryService.updateById(any(Delivery.class))).thenReturn(false);

        assertThrows(
                DeliveryConcurrentUpdateException.class,
                () -> controller.updateDelivery(5L, new UpdateDeliveryRequest())
        );
    }

    @Test
    void updateDeliveryRejectsAPartialDestination() {
        when(deliveryService.getById(5L)).thenReturn(delivery(5L));
        UpdateDeliveryRequest request = new UpdateDeliveryRequest();
        request.setDestinationLongitude(new BigDecimal("-0.1"));

        assertThrows(
                InvalidDeliveryDestinationException.class,
                () -> controller.updateDelivery(5L, request)
        );
    }

    @Test
    void deleteDeliveryRemovesTheExistingId() {
        Delivery delivery = delivery(5L);
        when(deliveryService.getById(5L)).thenReturn(delivery);

        controller.deleteDelivery(5L);

        verify(deliveryService).removeById(5L);
    }

    @Test
    void workflowEndpointsDelegateActorAndDeliveryIds() {
        AppUser actor = new AppUser();
        AssignDeliveryRequest assignment = new AssignDeliveryRequest();
        assignment.setDriverId(8L);
        Delivery expected = delivery(5L);
        when(deliveryService.assignDriver(actor, 5L, 8L)).thenReturn(expected);
        when(deliveryService.autoAssignDriver(actor, 5L)).thenReturn(expected);
        when(deliveryService.confirmDelivery(actor, 5L)).thenReturn(expected);
        when(deliveryService.cancelDelivery(actor, 5L)).thenReturn(expected);
        when(deliveryService.markReturned(actor, 5L)).thenReturn(expected);

        assertSame(expected, controller.assignDriver(actor, 5L, assignment));
        assertSame(expected, controller.autoAssignDriver(actor, 5L));
        assertSame(expected, controller.confirmDelivery(actor, 5L));
        assertSame(expected, controller.cancelDelivery(actor, 5L));
        assertSame(expected, controller.markReturned(actor, 5L));
    }

    private AtomicReference<Delivery> captureSavedDelivery() {
        AtomicReference<Delivery> saved = new AtomicReference<>();
        when(deliveryService.save(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(5L);
            delivery.setCreatedAt(new Date(1_700_000_000_000L));
            saved.set(delivery);
            return true;
        });
        when(deliveryService.getById(5L)).thenAnswer(invocation -> saved.get());
        return saved;
    }

    private LambdaQueryWrapper<Delivery> mockQuery() {
        LambdaQueryWrapper<Delivery> query =
                org.mockito.Mockito.mock(
                        LambdaQueryWrapper.class,
                        Answers.RETURNS_SELF
                );
        when(query.orderByDesc(
                org.mockito.ArgumentMatchers
                        .<SFunction<Delivery, ?>>any()
        )).thenReturn(query);
        return query;
    }

    private UpdateDeliveryRequest completeUpdateRequest() {
        UpdateDeliveryRequest request = new UpdateDeliveryRequest();
        request.setCustomerName("Updated");
        request.setCustomerPhone("456");
        request.setAddress("Manchester");
        request.setPickupLatitude(new BigDecimal("53.48"));
        request.setPickupLongitude(new BigDecimal("-2.24"));
        request.setDestinationLatitude(new BigDecimal("53.49"));
        request.setDestinationLongitude(new BigDecimal("-2.25"));
        request.setPriority(DeliveryPriority.URGENT);
        request.setPackageWeightKg(new BigDecimal("7.00"));
        Date start = new Date(System.currentTimeMillis() + 60_000);
        request.setTimeWindowStart(start);
        request.setTimeWindowEnd(new Date(start.getTime() + 60_000));
        return request;
    }

    private Delivery delivery(Long id) {
        Delivery delivery = new Delivery();
        delivery.setId(id);
        return delivery;
    }
}
