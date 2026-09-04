package com.zachary.delivery_system.service.assignment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.enums.DeliveryPriority;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryPickupMissingException;
import com.zachary.delivery_system.exception.DeliveryTimeWindowExpiredException;
import com.zachary.delivery_system.exception.NoEligibleDriverException;
import com.zachary.delivery_system.mapper.DeliveryMapper;
import com.zachary.delivery_system.mapper.DriverMapper;
import com.zachary.delivery_system.service.DriverLocationService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAssignmentServiceTest {

    @BeforeAll
    static void initializeMyBatisTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, "unit-test");

        TableInfoHelper.initTableInfo(assistant, Driver.class);
        TableInfoHelper.initTableInfo(assistant, Delivery.class);
    }

    @Mock
    private DriverMapper driverMapper;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DriverLocationService driverLocationService;

    private DriverAssignmentService assignmentService;

    @BeforeEach
    void setUp() {
        assignmentService = new DriverAssignmentService(
                driverMapper,
                deliveryMapper,
                driverLocationService,
                10,
                300
        );
    }

    @Test
    void selectsOneDriverUsingDistanceAndCurrentWorkload() {
        Driver closeButBusy = driver(1L, true, true, "100.00");
        Driver fartherButIdle = driver(2L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(closeButBusy, fartherButIdle));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(
                        activeDelivery(1L, "1.00"),
                        activeDelivery(1L, "1.00"),
                        activeDelivery(1L, "1.00"),
                        activeDelivery(1L, "1.00")
                ));
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0.000000", "0.010000", Instant.now()),
                location(2L, "0.000000", "0.050000", Instant.now())
        ));

        DriverAssignmentDecision result =
                assignmentService.selectBestDriver(delivery);

        assertEquals(2L, result.driver().getId());
        assertEquals(0, result.activeDeliveryCount());
    }

    @Test
    void excludesUnavailableAndOverCapacityDrivers() {
        Driver unavailable = driver(1L, true, false, "100.00");
        Driver overCapacity = driver(2L, true, true, "10.00");
        Driver eligible = driver(3L, true, true, "20.00");
        Delivery delivery = deliveryToAssign("2.00");

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(unavailable, overCapacity, eligible));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(activeDelivery(2L, "9.00")));
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0.000000", "0.001000", Instant.now()),
                location(2L, "0.000000", "0.002000", Instant.now()),
                location(3L, "0.000000", "0.003000", Instant.now())
        ));

        DriverAssignmentDecision result =
                assignmentService.selectBestDriver(delivery);

        assertEquals(3L, result.driver().getId());
    }

    @Test
    void urgentDeliveryWithShortWindowGivesMoreWeightToDistance() {
        Driver closeButBusy = driver(1L, true, true, "100.00");
        Driver fartherButIdle = driver(2L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");
        delivery.setPriority(DeliveryPriority.URGENT);
        delivery.setTimeWindowStart(Date.from(Instant.now()));
        delivery.setTimeWindowEnd(
                Date.from(Instant.now().plusSeconds(30 * 60))
        );

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(closeButBusy, fartherButIdle));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(activeDelivery(1L, "1.00")));
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0.000000", "0.010000", Instant.now()),
                location(2L, "0.000000", "0.040000", Instant.now())
        ));

        DriverAssignmentDecision result =
                assignmentService.selectBestDriver(delivery);

        assertEquals(1L, result.driver().getId());
    }

    @Test
    void rejectsDriversWhoseLocationsAreStale() {
        Driver driver = driver(1L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(driver));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(
                        1L,
                        "0.000000",
                        "0.001000",
                        Instant.now().minusSeconds(301)
                )
        ));

        assertThrows(
                NoEligibleDriverException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void rejectsDeliveryWithoutPickupCoordinates() {
        Delivery delivery = deliveryToAssign("1.00");
        delivery.setPickupLatitude(null);
        delivery.setPickupLongitude(null);

        assertThrows(
                DeliveryPickupMissingException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void excludesDriverAtMaximumWorkload() {
        Driver driver = driver(1L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");
        List<Delivery> activeDeliveries = IntStream.range(0, 10)
                .mapToObj(index -> activeDelivery(1L, "1.00"))
                .toList();

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(driver));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(activeDeliveries);
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0.000000", "0.001000", Instant.now())
        ));

        assertThrows(
                NoEligibleDriverException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void rejectsWhenTheDatabaseReturnsNoAvailableDrivers() {
        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        assertThrows(
                NoEligibleDriverException.class,
                () -> assignmentService.selectBestDriver(
                        deliveryToAssign("1.00")
                )
        );
    }

    @Test
    void rejectsDeliveryWithOnlyOnePickupCoordinate() {
        Delivery delivery = deliveryToAssign("1.00");
        delivery.setPickupLongitude(null);

        assertThrows(
                DeliveryPickupMissingException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void rejectsDeliveryWhoseTimeWindowHasExpired() {
        Delivery delivery = deliveryToAssign("1.00");
        delivery.setTimeWindowEnd(
                Date.from(Instant.now().minusSeconds(60))
        );

        assertThrows(
                DeliveryTimeWindowExpiredException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void filtersLocationsWithAnyRequiredFieldMissing() {
        Driver driver = driver(1L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");
        Instant now = Instant.now();

        DriverLatestLocationResponse missingDriver =
                location(1L, "0", "0", now);
        missingDriver.setDriverId(null);
        DriverLatestLocationResponse missingReceivedAt =
                location(1L, "0", "0", now);
        missingReceivedAt.setReceivedAt(null);
        DriverLatestLocationResponse missingLatitude =
                location(1L, "0", "0", now);
        missingLatitude.setLatitude(null);
        DriverLatestLocationResponse missingLongitude =
                location(1L, "0", "0", now);
        missingLongitude.setLongitude(null);

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(driver));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                missingDriver,
                missingReceivedAt,
                missingLatitude,
                missingLongitude
        ));

        assertThrows(
                NoEligibleDriverException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void keepsTheNewestLocationWhenADriverHasDuplicates() {
        Driver driver = driver(1L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");
        Instant now = Instant.now();

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(driver));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0", "0.030000", now.minusSeconds(3)),
                location(1L, "0", "0.010000", now),
                location(1L, "0", "0.020000", now.minusSeconds(1))
        ));

        DriverAssignmentDecision result =
                assignmentService.selectBestDriver(delivery);

        assertEquals(1L, result.driver().getId());
        // The newest point is closest to the pickup at longitude zero.
        org.junit.jupiter.api.Assertions.assertEquals(
                1.111,
                result.distanceKm(),
                0.01
        );
    }

    @Test
    void excludesDriversWithMissingOrNonPositiveCapacity() {
        Driver missingCapacity = driver(1L, true, true, "100.00");
        missingCapacity.setVehicleCapacityKg(null);
        Driver zeroCapacity = driver(2L, true, true, "0.00");
        Delivery delivery = deliveryToAssign("1.00");

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(missingCapacity, zeroCapacity));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0", "0.001", Instant.now()),
                location(2L, "0", "0.002", Instant.now())
        ));

        assertThrows(
                NoEligibleDriverException.class,
                () -> assignmentService.selectBestDriver(delivery)
        );
    }

    @Test
    void defaultsMissingPriorityAndWeightAndHandlesAFutureWindow() {
        Driver driver = driver(1L, true, true, "100.00");
        Delivery delivery = deliveryToAssign("1.00");
        delivery.setPriority(null);
        delivery.setPackageWeightKg(null);
        delivery.setTimeWindowStart(
                Date.from(Instant.now().plusSeconds(5 * 60 * 60))
        );
        delivery.setTimeWindowEnd(
                Date.from(Instant.now().plusSeconds(6 * 60 * 60))
        );

        when(driverMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(driver));
        when(deliveryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());
        when(driverLocationService.getLatestLocations()).thenReturn(List.of(
                location(1L, "0", "0.001", Instant.now())
        ));

        DriverAssignmentDecision result =
                assignmentService.selectBestDriver(delivery);

        assertEquals(1L, result.driver().getId());
        assertEquals(BigDecimal.ZERO, result.activeLoadKg());
    }

    private Delivery deliveryToAssign(String packageWeightKg) {
        Delivery delivery = new Delivery();
        delivery.setId(5L);
        delivery.setPickupLatitude(BigDecimal.ZERO);
        delivery.setPickupLongitude(BigDecimal.ZERO);
        delivery.setPackageWeightKg(new BigDecimal(packageWeightKg));
        delivery.setPriority(DeliveryPriority.NORMAL);
        delivery.setStatus(DeliveryStatus.CREATED);
        return delivery;
    }

    private Delivery activeDelivery(Long driverId, String packageWeightKg) {
        Delivery delivery = new Delivery();
        delivery.setDriverId(driverId);
        delivery.setPackageWeightKg(new BigDecimal(packageWeightKg));
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        return delivery;
    }

    private Driver driver(
            Long id,
            boolean active,
            boolean available,
            String capacityKg
    ) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setActive(active);
        driver.setAvailable(available);
        driver.setVehicleCapacityKg(new BigDecimal(capacityKg));
        return driver;
    }

    private DriverLatestLocationResponse location(
            Long driverId,
            String latitude,
            String longitude,
            Instant receivedAt
    ) {
        DriverLatestLocationResponse location =
                new DriverLatestLocationResponse();
        location.setDriverId(driverId);
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        location.setRecordedAt(Date.from(receivedAt));
        location.setReceivedAt(Date.from(receivedAt));
        return location;
    }
}
