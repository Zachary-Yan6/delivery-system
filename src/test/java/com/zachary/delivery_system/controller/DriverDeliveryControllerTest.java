package com.zachary.delivery_system.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.DriverAccountUnavailableException;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverDeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;
    @Mock
    private DriverService driverService;
    @Mock
    private LambdaQueryChainWrapper<Driver> driverQuery;

    private DriverDeliveryController controller;
    private AppUser currentUser;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        controller = new DriverDeliveryController(
                deliveryService,
                driverService
        );
        currentUser = new AppUser();
        currentUser.setId(3L);
        delivery = new Delivery();
        delivery.setId(5L);
    }

    @Test
    void everyDriverWorkflowEndpointUsesTheAuthenticatedActiveDriver() {
        stubCurrentDriver(activeDriver(8L));
        when(deliveryService.getDeliveriesForDriver(8L))
                .thenReturn(List.of(delivery));
        when(deliveryService.acceptDelivery(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.pickupDelivery(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.startDelivery(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.markDelivered(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.markFailed(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.retryDelivery(currentUser, 8L, 5L))
                .thenReturn(delivery);
        when(deliveryService.resumeDelivery(currentUser, 8L, 5L))
                .thenReturn(delivery);

        assertEquals(List.of(delivery), controller.getMyDeliveries(currentUser));
        assertSame(delivery, controller.acceptDelivery(currentUser, 5L));
        assertSame(delivery, controller.pickupDelivery(currentUser, 5L));
        assertSame(delivery, controller.startDelivery(currentUser, 5L));
        assertSame(delivery, controller.markDelivered(currentUser, 5L));
        assertSame(delivery, controller.markFailed(currentUser, 5L));
        assertSame(delivery, controller.retryDelivery(currentUser, 5L));
        assertSame(delivery, controller.resumeDelivery(currentUser, 5L));

        verify(deliveryService).getDeliveriesForDriver(8L);
        verify(deliveryService).resumeDelivery(currentUser, 8L, 5L);
    }

    @Test
    void endpointRejectsAnAccountWithoutADriverRecord() {
        stubCurrentDriver(null);

        assertThrows(
                DriverAccountUnavailableException.class,
                () -> controller.getMyDeliveries(currentUser)
        );
        verifyNoInteractions(deliveryService);
    }

    @Test
    void endpointRejectsAnInactiveDriver() {
        Driver inactive = activeDriver(8L);
        inactive.setActive(false);
        stubCurrentDriver(inactive);

        assertThrows(
                DriverAccountUnavailableException.class,
                () -> controller.startDelivery(currentUser, 5L)
        );
        verifyNoInteractions(deliveryService);
    }

    private void stubCurrentDriver(Driver driver) {
        when(driverService.lambdaQuery()).thenReturn(driverQuery);
        when(driverQuery.eq(any(), any())).thenReturn(driverQuery);
        when(driverQuery.one()).thenReturn(driver);
    }

    private Driver activeDriver(Long id) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setActive(true);
        return driver;
    }
}
