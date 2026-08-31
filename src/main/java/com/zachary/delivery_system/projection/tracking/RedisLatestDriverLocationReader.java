package com.zachary.delivery_system.projection.tracking;

import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLatestDriverLocationReader {

    private static final String KEY_PATTERN = "tracking:driver:*:latest";

    private final StringRedisTemplate redisTemplate;
    private final DriverService driverService;

    public List<DriverLatestLocationResponse> readLatestLocations() {
        List<Map<Object, Object>> cachedLocations = new ArrayList<>();

        // scan all keys match to KEY_PATTERN without blocking the redis
        // redisTemplate.keys would block the redis cause it would scan the whole redis
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(KEY_PATTERN)
                        // at most 100 per batch
                        .count(100)
                        .build()
        )) {
            // query and store all driver latest location
            while (cursor.hasNext()) {
                Map<Object, Object> location = redisTemplate.opsForHash()
                        .entries(cursor.next());

                if (!location.isEmpty()) {
                    cachedLocations.add(location);
                }
            }
        }

        if (cachedLocations.isEmpty()) {
            return List.of();
        }

        // all driver id
        Set<Long> driverIds = new HashSet<>();

        for (Map<Object, Object> location : cachedLocations) {
            try {
                driverIds.add(Long.valueOf(valueOf(location, "driverId")));
            } catch (RuntimeException exception) {
                log.warn("Ignoring invalid Redis location entry", exception);
            }
        }

        if (driverIds.isEmpty()) {
            return List.of();
        }

        // all active driver
        // <id, name>
        Map<Long, String> activeDriverNames = new HashMap<>();

        for (Driver driver : driverService.lambdaQuery()
                .in(Driver::getId, driverIds)
                .eq(Driver::getActive, true)
                .list()) {

            activeDriverNames.put(driver.getId(), driver.getFullName());
        }

        // all active driver latest location
        List<DriverLatestLocationResponse> responses = new ArrayList<>();

        for (Map<Object, Object> location : cachedLocations) {
            try {
                DriverLatestLocationResponse response = toResponse(location);
                String driverName = activeDriverNames.get(response.getDriverId());

                if (driverName != null) {
                    response.setDriverName(driverName);
                    responses.add(response);
                }
            } catch (RuntimeException exception) {
                log.warn("Ignoring invalid Redis location entry", exception);
            }
        }

        return responses;
    }

    private DriverLatestLocationResponse toResponse(Map<Object, Object> location) {
        DriverLatestLocationResponse response = new DriverLatestLocationResponse();

        response.setDriverId(Long.valueOf(valueOf(location, "driverId")));
        response.setLatitude(new BigDecimal(valueOf(location, "latitude")));
        response.setLongitude(new BigDecimal(valueOf(location, "longitude")));
        response.setRecordedAt(
                Date.from(Instant.parse(valueOf(location, "recordedAt")))
        );
        response.setReceivedAt(
                Date.from(Instant.parse(valueOf(location, "receivedAt")))
        );

        return response;
    }

    private String valueOf(Map<Object, Object> location, String fieldName) {
        Object value = location.get(fieldName);

        if (value == null) {
            throw new IllegalStateException(
                    "Redis location is missing field: " + fieldName
            );
        }

        return value.toString();
    }
}