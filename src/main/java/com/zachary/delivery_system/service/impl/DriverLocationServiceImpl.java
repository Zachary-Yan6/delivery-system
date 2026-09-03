package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
import com.zachary.delivery_system.service.impl.DriverLocationOutboxService;
import com.zachary.delivery_system.projection.tracking.RedisLatestDriverLocationReader;
import com.zachary.delivery_system.service.DriverLocationService;
import com.zachary.delivery_system.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl
        extends ServiceImpl<DriverLocationMapper, DriverLocation>
        implements DriverLocationService {

    private static final long MINIMUM_UPDATE_INTERVAL_MS = 5_000;
    private static final long MAX_FUTURE_TIME_MS = 5 * 60 * 1_000;

    private final DriverService driverService;
    private final DriverLocationMapper driverLocationMapper;
    private final RedisLatestDriverLocationReader
            redisLatestDriverLocationReader;
    private final DriverLocationOutboxService outboxService;

    @Override
    @Transactional
    public DriverLocation recordLocation(
            AppUser currentUser,
            DriverLocationRequest request
    ) {
        /*
         * This lock serializes location requests for the same driver.
         * Different drivers can still upload at the same time.
         */
        Driver driver = findDriverForUpdate(currentUser);

        if (driver == null
                || !Boolean.TRUE.equals(driver.getActive())) {
            throw new DriverLocationAccessDeniedException();
        }

        Date now = new Date();

        if (request.getRecordedAt().after(
                new Date(now.getTime() + MAX_FUTURE_TIME_MS)
        )) {
            throw new InvalidLocationTimestampException();
        }

        DriverLocation latestLocation = this.lambdaQuery()
                .eq(DriverLocation::getDriverId, driver.getId())
                .orderByDesc(DriverLocation::getReceivedAt)
                .last("LIMIT 1")
                .one();

        if (latestLocation != null
                && now.getTime()
                - latestLocation.getReceivedAt().getTime()
                < MINIMUM_UPDATE_INTERVAL_MS) {
            throw new LocationUpdateRateLimitException();
        }

        DriverLocation location = new DriverLocation();
        location.setDriverId(driver.getId());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRecordedAt(request.getRecordedAt());
        location.setReceivedAt(now);

        /*
         * Both inserts happen inside the same PostgreSQL transaction.
         */
        this.save(location);

        DriverLocationReportedEvent event =
                new DriverLocationReportedEvent(
                        UUID.randomUUID(),
                        driver.getId(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getRecordedAt().toInstant(),
                        location.getReceivedAt().toInstant()
                );

        outboxService.savePending(event);

        /*
         * Do not call Kafka here.
         * The background Outbox Publisher sends the event after commit.
         */
        return location;
    }

    @Override
    public List<DriverLatestLocationResponse> getLatestLocations() {
        try {
            return redisLatestDriverLocationReader
                    .readLatestLocations();
        } catch (RedisConnectionFailureException exception) {
            log.warn(
                    "Redis is unavailable; falling back to PostgreSQL",
                    exception
            );

            return driverLocationMapper.selectLatestLocations();
        }
    }

    private Driver findDriverForUpdate(AppUser currentUser) {
        return driverService.lambdaQuery()
                .eq(Driver::getUserId, currentUser.getId())
                .last("FOR UPDATE")
                .one();
    }
}