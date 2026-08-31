package com.zachary.delivery_system.alert;

import com.zachary.delivery_system.dto.Location.DriverStaleLocationCandidate;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleDriverAlertScheduler {

    private static final Duration STALE_AFTER = Duration.ofSeconds(90);
    private static final Duration ALERT_TTL = Duration.ofMinutes(5);

    private final DriverLocationEventsMapper driverLocationEventsMapper;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(
            initialDelay = 15,
            fixedDelay = 30,
            timeUnit = TimeUnit.SECONDS
    )
    public void detectStaleDrivers() {
        Instant cutoff = Instant.now().minus(STALE_AFTER);

        for (DriverStaleLocationCandidate driver :
                driverLocationEventsMapper.selectDriversWithStaleLocation(cutoff)) {

            createAlertIfAbsent(driver);
        }
    }

    private void createAlertIfAbsent(DriverStaleLocationCandidate driver) {
        String alertKey =
                "tracking:alert:driver:" + driver.getDriverId() + ":stale";

        String lockKey = alertKey + ":lock";

        Boolean created = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", ALERT_TTL);

        if (!Boolean.TRUE.equals(created)) {
            return;
        }

        Map<String, String> alert = Map.of(
                "driverId", driver.getDriverId().toString(),
                "driverName", driver.getDriverName(),
                "type", "LOCATION_STALE",
                "message", "Driver has not reported a location for over 90 seconds.",
                "lastReceivedAt", driver.getLastReceivedAt().toInstant().toString(),
                "raisedAt", Instant.now().toString()
        );

        redisTemplate.opsForHash().putAll(alertKey, alert);
        redisTemplate.expire(alertKey, ALERT_TTL);

        log.warn(
                "Created stale-location alert for driver {}",
                driver.getDriverId()
        );
    }
}