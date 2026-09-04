package com.zachary.delivery_system.security;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.DeliveryAccessDeniedException;
import com.zachary.delivery_system.service.AppUserService;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAuthorizationServiceTest {

    @Mock
    private DriverService driverService;

    @Mock
    private AppUserService appUserService;

    @Mock
    private LambdaQueryChainWrapper<Driver> driverQuery;

    @Test
    void customerCanViewTheirOwnDelivery() {
        DeliveryAuthorizationService service = service();

        assertDoesNotThrow(() -> service.assertCanView(
                user(10L, "CUSTOMER"), delivery(10L, 8L)
        ));
    }

    @Test
    void customerCannotViewAnotherCustomersDelivery() {
        DeliveryAuthorizationService service = service();

        DeliveryAccessDeniedException exception = assertThrows(
                DeliveryAccessDeniedException.class,
                () -> service.assertCanView(user(10L, "CUSTOMER"), delivery(11L, 8L))
        );

        assertEquals("DELIVERY_ACCESS_DENIED", exception.getCode());
    }

    @Test
    void dispatcherCanViewAnyDelivery() {
        DeliveryAuthorizationService service = service();

        assertDoesNotThrow(() -> service.assertCanView(
                user(2L, "DISPATCHER"), delivery(10L, 8L)
        ));
    }

    @Test
    void assignedActiveDriverCanViewTheirDelivery() {
        Driver driver = new Driver();
        driver.setId(8L);
        driver.setActive(true);

        when(driverService.lambdaQuery()).thenReturn(driverQuery);
        when(driverQuery.eq(any(), any())).thenReturn(driverQuery);
        when(driverQuery.one()).thenReturn(driver);

        assertDoesNotThrow(() -> service().assertCanView(
                user(20L, "DRIVER"), delivery(10L, 8L)
        ));
    }

    @Test
    void unassignedDriverCannotViewDelivery() {
        Driver driver = new Driver();
        driver.setId(9L);
        driver.setActive(true);

        when(driverService.lambdaQuery()).thenReturn(driverQuery);
        when(driverQuery.eq(any(), any())).thenReturn(driverQuery);
        when(driverQuery.one()).thenReturn(driver);

        assertThrows(
                DeliveryAccessDeniedException.class,
                () -> service().assertCanView(user(20L, "DRIVER"), delivery(10L, 8L))
        );
    }

    private DeliveryAuthorizationService service() {
        return new DeliveryAuthorizationService(driverService, appUserService);
    }

    private AppUser user(Long id, String role) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Delivery delivery(Long ownerId, Long driverId) {
        Delivery delivery = new Delivery();
        delivery.setOwnerId(ownerId);
        delivery.setDriverId(driverId);
        return delivery;
    }
}
