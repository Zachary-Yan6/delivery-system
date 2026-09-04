package com.zachary.delivery_system.projection.tracking;

import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.websocket.DriverLocationWebSocketPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationTrackingConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private DriverLocationWebSocketPublisher webSocketPublisher;

    @Test
    void updateLatestLocation_writesTheLatestLocationWithATtlAndClearsStaleAlert() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        DriverLocationTrackingConsumer consumer =
                new DriverLocationTrackingConsumer(
                        redisTemplate,
                        webSocketPublisher
                );
        DriverLocationReportedEvent event = new DriverLocationReportedEvent(
                UUID.fromString("b2b2a42e-0d5b-4fb1-8f4d-3285587b6b71"),
                17L,
                new BigDecimal("51.507400"),
                new BigDecimal("-0.127800"),
                Instant.parse("2026-08-31T10:00:00Z"),
                Instant.parse("2026-08-31T10:00:05Z")
        );

        consumer.updateLatestLocation(event);

        ArgumentCaptor<Map<Object, Object>> locationCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(
                eq("tracking:driver:17:latest"),
                locationCaptor.capture()
        );
        assertEquals("17", locationCaptor.getValue().get("driverId"));
        assertEquals("51.507400", locationCaptor.getValue().get("latitude"));
        assertEquals("-0.127800", locationCaptor.getValue().get("longitude"));
        assertEquals("2026-08-31T10:00:05Z", locationCaptor.getValue().get("receivedAt"));
        verify(redisTemplate).expire(
                "tracking:driver:17:latest",
                Duration.ofSeconds(90)
        );
        verify(redisTemplate).delete("tracking:alert:driver:17:stale");
        verify(redisTemplate).delete("tracking:alert:driver:17:stale:lock");
        verify(webSocketPublisher).publish(event);
    }
}
