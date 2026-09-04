package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Location.DriverLocationAlertResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatcherAlertServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private Cursor<String> cursor;

    @Test
    void getActiveAlertsSkipsEmptyAndInvalidEntriesAndSortsNewestFirst() {
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(cursor.hasNext()).thenReturn(
                true,
                true,
                true,
                true,
                true,
                false
        );
        when(cursor.next()).thenReturn(
                "older",
                "empty",
                "invalid",
                "missing-field",
                "newer"
        );
        when(hashOperations.entries("older")).thenReturn(alert(
                "1",
                "Driver One",
                "2026-09-01T10:00:00Z",
                "2026-09-01T10:01:00Z"
        ));
        when(hashOperations.entries("empty")).thenReturn(Map.of());
        when(hashOperations.entries("invalid")).thenReturn(Map.of(
                "driverId", "broken"
        ));
        when(hashOperations.entries("missing-field")).thenReturn(Map.of(
                "driverName", "Driver Without Id"
        ));
        when(hashOperations.entries("newer")).thenReturn(alert(
                "2",
                "Driver Two",
                "2026-09-01T10:02:00Z",
                "2026-09-01T10:03:00Z"
        ));

        List<DriverLocationAlertResponse> result =
                new DispatcherAlertServiceImpl(redisTemplate)
                        .getActiveAlerts();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getDriverId());
        assertEquals("Driver Two", result.get(0).getDriverName());
        assertEquals("STALE_LOCATION", result.get(0).getType());
        assertEquals("Driver location is stale", result.get(0).getMessage());
        assertEquals(1L, result.get(1).getDriverId());
        verify(cursor).close();
    }

    private Map<Object, Object> alert(
            String driverId,
            String driverName,
            String lastReceivedAt,
            String raisedAt
    ) {
        return Map.of(
                "driverId", driverId,
                "driverName", driverName,
                "type", "STALE_LOCATION",
                "message", "Driver location is stale",
                "lastReceivedAt", lastReceivedAt,
                "raisedAt", raisedAt
        );
    }
}
