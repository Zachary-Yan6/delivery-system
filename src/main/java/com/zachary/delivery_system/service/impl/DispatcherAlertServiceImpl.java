package com.zachary.delivery_system.service.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.zachary.delivery_system.dto.Location.DriverLocationAlertResponse;
import com.zachary.delivery_system.service.DispatcherAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DispatcherAlertServiceImpl implements DispatcherAlertService {

    private static final String ALERT_KEY_PATTERN =
            "tracking:alert:driver:*:stale";

    private final StringRedisTemplate redisTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "StringRedisTemplate is a Spring-managed dependency that is intentionally shared."
    )
    public DispatcherAlertServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<DriverLocationAlertResponse> getActiveAlerts() {
        List<DriverLocationAlertResponse> alerts = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                        .match(ALERT_KEY_PATTERN)
                        .count(100)
                        .build()
        )) {
            while (cursor.hasNext()) {
                Map<Object, Object> storedAlert = redisTemplate.opsForHash()
                        .entries(cursor.next());

                if (storedAlert.isEmpty()) {
                    continue;
                }

                try {
                    alerts.add(toResponse(storedAlert));
                } catch (RuntimeException exception) {
                    log.warn("Ignoring invalid Redis alert entry", exception);
                }
            }
        }

        alerts.sort(
                Comparator.comparing(
                        DriverLocationAlertResponse::getRaisedAt
                ).reversed()
        );

        return alerts;
    }

    private DriverLocationAlertResponse toResponse(
            Map<Object, Object> storedAlert
    ) {
        DriverLocationAlertResponse response =
                new DriverLocationAlertResponse();

        response.setDriverId(
                Long.valueOf(valueOf(storedAlert, "driverId"))
        );
        response.setDriverName(valueOf(storedAlert, "driverName"));
        response.setType(valueOf(storedAlert, "type"));
        response.setMessage(valueOf(storedAlert, "message"));
        response.setLastReceivedAt(
                Date.from(
                        Instant.parse(valueOf(storedAlert, "lastReceivedAt"))
                )
        );
        response.setRaisedAt(
                Date.from(
                        Instant.parse(valueOf(storedAlert, "raisedAt"))
                )
        );

        return response;
    }

    private String valueOf(
            Map<Object, Object> storedAlert,
            String fieldName
    ) {
        Object value = storedAlert.get(fieldName);

        if (value == null) {
            throw new IllegalStateException(
                    "Redis alert is missing field: " + fieldName
            );
        }

        return value.toString();
    }
}
