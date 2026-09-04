package com.zachary.delivery_system.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zachary.delivery_system.dto.Driver.CreateDriverRequest;
import com.zachary.delivery_system.dto.Driver.UpdateDriverAvailabilityRequest;
import com.zachary.delivery_system.dto.Driver.UpdateDriverRequest;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.DriverHasActiveDeliveriesException;
import com.zachary.delivery_system.exception.DriverNotFoundException;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverService driverService;
    @Mock
    private DeliveryService deliveryService;
    @Mock
    private LambdaQueryChainWrapper<Delivery> deliveryQuery;

    private DriverController controller;

    @BeforeEach
    void setUp() {
        controller = new DriverController(driverService, deliveryService);
    }

    @Test
    void createAndListDelegateToTheDriverService() {
        CreateDriverRequest request = new CreateDriverRequest();
        Driver driver = driver(8L);
        when(driverService.createDriver(request)).thenReturn(driver);
        when(driverService.list()).thenReturn(List.of(driver));

        assertSame(driver, controller.createDriver(request));
        assertEquals(List.of(driver), controller.getDrivers());
    }

    @Test
    void updateDriverChangesAllProvidedEditableFields() {
        Driver driver = driver(8L);
        when(driverService.getById(8L)).thenReturn(driver);
        UpdateDriverRequest request = new UpdateDriverRequest();
        request.setFullName("New Name");
        request.setPhone("0800000000");
        request.setVehicleCapacityKg(new BigDecimal("250.00"));
        request.setAvailable(false);

        Driver result = controller.updateDriver(8L, request);

        assertSame(driver, result);
        assertEquals("New Name", result.getFullName());
        assertEquals("0800000000", result.getPhone());
        assertEquals(new BigDecimal("250.00"), result.getVehicleCapacityKg());
        assertFalse(result.getAvailable());
        verify(driverService).updateById(driver);
    }

    @Test
    void updateDriverKeepsOptionalFieldsWhenTheyAreOmitted() {
        Driver driver = driver(8L);
        driver.setVehicleCapacityKg(new BigDecimal("100.00"));
        driver.setAvailable(true);
        when(driverService.getById(8L)).thenReturn(driver);
        UpdateDriverRequest request = new UpdateDriverRequest();
        request.setFullName("Name");
        request.setPhone("Phone");

        Driver result = controller.updateDriver(8L, request);

        assertEquals(new BigDecimal("100.00"), result.getVehicleCapacityKg());
        assertTrue(result.getAvailable());
    }

    @Test
    void availabilityAndActivationUpdateTheDriver() {
        Driver driver = driver(8L);
        driver.setActive(false);
        when(driverService.getById(8L)).thenReturn(driver);
        UpdateDriverAvailabilityRequest request =
                new UpdateDriverAvailabilityRequest();
        request.setAvailable(false);

        assertSame(driver, controller.updateAvailability(8L, request));
        assertFalse(driver.getAvailable());
        assertSame(driver, controller.activateDriver(8L));
        assertTrue(driver.getActive());
        verify(driverService, org.mockito.Mockito.times(2))
                .updateById(driver);
    }

    @Test
    void deactivateMarksDriverInactiveWhenNoActiveDeliveryExists() {
        Driver driver = driver(8L);
        driver.setActive(true);
        when(driverService.getById(8L)).thenReturn(driver);
        stubActiveDeliveryExists(false);

        controller.deactivateDriver(8L);

        assertFalse(driver.getActive());
        verify(driverService).updateById(driver);
    }

    @Test
    void deactivateRejectsDriverWithActiveDeliveries() {
        Driver driver = driver(8L);
        when(driverService.getById(8L)).thenReturn(driver);
        stubActiveDeliveryExists(true);

        assertThrows(
                DriverHasActiveDeliveriesException.class,
                () -> controller.deactivateDriver(8L)
        );
        verify(driverService, never()).updateById(driver);
    }

    @Test
    void getDriverReturnsExistingDriverAndRejectsMissingDriver() {
        Driver driver = driver(8L);
        when(driverService.getById(8L)).thenReturn(driver);
        when(driverService.getById(9L)).thenReturn(null);

        assertSame(driver, controller.getDriver(8L));
        assertThrows(DriverNotFoundException.class,
                () -> controller.getDriver(9L));
    }

    private void stubActiveDeliveryExists(boolean exists) {
        when(deliveryService.lambdaQuery()).thenReturn(deliveryQuery);
        when(deliveryQuery.eq(any(), any())).thenReturn(deliveryQuery);
        when(deliveryQuery.in(any(), anyCollection()))
                .thenReturn(deliveryQuery);
        when(deliveryQuery.exists()).thenReturn(exists);
    }

    private Driver driver(Long id) {
        Driver driver = new Driver();
        driver.setId(id);
        return driver;
    }
}
