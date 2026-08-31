package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DriverService driverService;

    @Test
    void assignDriver_assignsAnActiveDriverToACreatedDelivery() {
        Delivery delivery = deliveryWithStatus("CREATED");
        Driver driver = activeDriver(8L);
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(driver);

        Delivery result = service.assignDriver(5L, 8L);

        assertEquals(8L, result.getDriverId());
        assertEquals("ASSIGNED", result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(service).updateById(delivery);
    }

    @Test
    void assignDriver_rejectsADeliveryThatIsNotCreated() {
        Delivery delivery = deliveryWithStatus("DELIVERED");
        DeliveryServiceImpl service = serviceReturning(delivery);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.assignDriver(5L, 8L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Only a CREATED delivery can be assigned", exception.getReason());
        verifyNoInteractions(driverService);
    }

    @Test
    void markDelivered_changesAnInTransitDeliveryAndSetsDeliveredTime() {
        Delivery delivery = deliveryWithStatus("IN_TRANSIT");
        delivery.setDriverId(8L);
        DeliveryServiceImpl service = serviceReturning(delivery);

        when(driverService.getById(8L)).thenReturn(activeDriver(8L));

        Delivery result = service.markDelivered(8L, 5L);

        assertEquals("DELIVERED", result.getStatus());
        assertNotNull(result.getDeliveredAt());
        assertNotNull(result.getUpdatedAt());
        verify(service).updateById(delivery);
    }

    private DeliveryServiceImpl serviceReturning(Delivery delivery) {
        DeliveryServiceImpl service = spy(new DeliveryServiceImpl(driverService));
        doReturn(delivery).when(service).getById(5L);
        doReturn(true).when(service).updateById(any(Delivery.class));
        return service;
    }

    private Delivery deliveryWithStatus(String status) {
        Delivery delivery = new Delivery();
        delivery.setId(5L);
        delivery.setStatus(status);
        return delivery;
    }

    private Driver activeDriver(Long driverId) {
        Driver driver = new Driver();
        driver.setId(driverId);
        driver.setActive(true);
        return driver;
    }
}
