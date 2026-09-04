package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.exception.DriverLocationAccessDeniedException;
import com.zachary.delivery_system.exception.InvalidLocationTimestampException;
import com.zachary.delivery_system.exception.LocationUpdateRateLimitException;
import com.zachary.delivery_system.mapper.DriverLocationMapper;
import com.zachary.delivery_system.projection.tracking.RedisLatestDriverLocationReader;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationServiceImplTest {

    @Mock
    private DriverService driverService;
    @Mock
    private DriverLocationMapper driverLocationMapper;
    @Mock
    private RedisLatestDriverLocationReader redisReader;
    @Mock
    private DriverLocationOutboxService outboxService;
    @Mock
    private LambdaQueryChainWrapper<Driver> driverQuery;
    @Mock(answer = Answers.RETURNS_SELF)
    private LambdaQueryChainWrapper<DriverLocation> locationQuery;

    private DriverLocationServiceImpl service;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        service = spy(new DriverLocationServiceImpl(
                driverService,
                driverLocationMapper,
                redisReader,
                outboxService
        ));
        currentUser = new AppUser();
        currentUser.setId(3L);
    }

    @Test
    void recordLocationSavesLocationAndTransactionalOutboxEvent() {
        Driver driver = activeDriver();
        stubDriver(driver);
        stubLatestLocation(null);
        doReturn(true).when(service).save(any(DriverLocation.class));
        DriverLocationRequest request = request(new Date());

        DriverLocation result = service.recordLocation(currentUser, request);

        assertEquals(8L, result.getDriverId());
        assertEquals(request.getLatitude(), result.getLatitude());
        assertEquals(request.getLongitude(), result.getLongitude());
        assertSame(request.getRecordedAt(), result.getRecordedAt());
        assertNotNull(result.getReceivedAt());
        verify(service).save(result);
        ArgumentCaptor<DriverLocationReportedEvent> captor =
                ArgumentCaptor.forClass(DriverLocationReportedEvent.class);
        verify(outboxService).savePending(captor.capture());
        assertNotNull(captor.getValue().eventId());
        assertEquals(8L, captor.getValue().driverId());
        assertEquals(request.getLatitude(), captor.getValue().latitude());
        assertEquals(request.getLongitude(), captor.getValue().longitude());
        assertEquals(request.getRecordedAt().toInstant(),
                captor.getValue().recordedAt());
        assertEquals(result.getReceivedAt().toInstant(),
                captor.getValue().receivedAt());
    }

    @Test
    void recordLocationRejectsMissingAndInactiveDrivers() {
        stubDriver(null);
        assertThrows(
                DriverLocationAccessDeniedException.class,
                () -> service.recordLocation(currentUser, request(new Date()))
        );

        Driver inactive = activeDriver();
        inactive.setActive(false);
        stubDriver(inactive);
        assertThrows(
                DriverLocationAccessDeniedException.class,
                () -> service.recordLocation(currentUser, request(new Date()))
        );
    }

    @Test
    void recordLocationRejectsTimestampMoreThanFiveMinutesInFuture() {
        stubDriver(activeDriver());
        Date future = new Date(System.currentTimeMillis() + 6 * 60_000);

        assertThrows(
                InvalidLocationTimestampException.class,
                () -> service.recordLocation(currentUser, request(future))
        );

        verify(service, never()).save(any());
    }

    @Test
    void recordLocationRateLimitsTwoRequestsWithinFiveSeconds() {
        stubDriver(activeDriver());
        DriverLocation latest = new DriverLocation();
        latest.setReceivedAt(new Date());
        stubLatestLocation(latest);

        assertThrows(
                LocationUpdateRateLimitException.class,
                () -> service.recordLocation(currentUser, request(new Date()))
        );

        verify(outboxService, never()).savePending(any());
    }

    @Test
    void recordLocationAllowsAnOlderPreviousLocation() {
        stubDriver(activeDriver());
        DriverLocation latest = new DriverLocation();
        latest.setReceivedAt(new Date(System.currentTimeMillis() - 6_000));
        stubLatestLocation(latest);
        doReturn(true).when(service).save(any(DriverLocation.class));

        DriverLocation result =
                service.recordLocation(currentUser, request(new Date()));

        assertEquals(8L, result.getDriverId());
        verify(outboxService).savePending(any());
    }

    @Test
    void getLatestLocationsReadsTheRedisProjection() {
        List<DriverLatestLocationResponse> expected =
                List.of(new DriverLatestLocationResponse());
        when(redisReader.readLatestLocations()).thenReturn(expected);

        assertSame(expected, service.getLatestLocations());
        verify(driverLocationMapper, never()).selectLatestLocations();
    }

    @Test
    void getLatestLocationsFallsBackToPostgresWhenRedisIsUnavailable() {
        List<DriverLatestLocationResponse> expected =
                List.of(new DriverLatestLocationResponse());
        when(redisReader.readLatestLocations()).thenThrow(
                new RedisConnectionFailureException("Redis down")
        );
        when(driverLocationMapper.selectLatestLocations()).thenReturn(expected);

        assertSame(expected, service.getLatestLocations());
    }

    private void stubDriver(Driver driver) {
        when(driverService.lambdaQuery()).thenReturn(driverQuery);
        when(driverQuery.eq(any(), any())).thenReturn(driverQuery);
        when(driverQuery.last("FOR UPDATE")).thenReturn(driverQuery);
        when(driverQuery.one()).thenReturn(driver);
    }

    private void stubLatestLocation(DriverLocation location) {
        doReturn(locationQuery).when(service).lambdaQuery();
        when(locationQuery.eq(any(), any())).thenReturn(locationQuery);
        when(locationQuery.orderByDesc(
                org.mockito.ArgumentMatchers
                        .<SFunction<DriverLocation, ?>>any()
        )).thenReturn(locationQuery);
        when(locationQuery.last("LIMIT 1")).thenReturn(locationQuery);
        when(locationQuery.one()).thenReturn(location);
    }

    private Driver activeDriver() {
        Driver driver = new Driver();
        driver.setId(8L);
        driver.setActive(true);
        return driver;
    }

    private DriverLocationRequest request(Date recordedAt) {
        DriverLocationRequest request = new DriverLocationRequest();
        request.setLatitude(new BigDecimal("51.507400"));
        request.setLongitude(new BigDecimal("-0.127800"));
        request.setRecordedAt(recordedAt);
        return request;
    }
}
