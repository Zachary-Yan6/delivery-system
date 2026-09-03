package com.zachary.delivery_system.projection.tracking;

import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.websocket.DriverLocationWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationTrackingConsumer {

    private static final String KEY_PREFIX = "tracking:driver:";
    private static final Duration LOCATION_TTL = Duration.ofSeconds(90);
    private static final String STALE_ALERT_KEY_PREFIX = "tracking:alert:driver:";
    private final StringRedisTemplate redisTemplate;
    private final DriverLocationWebSocketPublisher webSocketPublisher;

    @KafkaListener(
            topics = "${app.kafka.topics.driver-location-reported}",
            groupId = "${app.kafka.groups.tracking-projection}"
    )
    public void updateLatestLocation(DriverLocationReportedEvent event) {
        String key = KEY_PREFIX + event.driverId() + ":latest";

        // Read the timestamp currently stored in the Redis projection.
        Object currentRecordedAtValue =
                redisTemplate.opsForHash().get(key, "recordedAt");

        if (currentRecordedAtValue != null) {
            Instant currentRecordedAt =
                    Instant.parse(currentRecordedAtValue.toString());

            // Ignore delayed or duplicate events so they cannot move a driver backward.
            if (!event.recordedAt().isAfter(currentRecordedAt)) {
                log.debug(
                        "Ignored stale location event {} for driver {}. " +
                                "event recordedAt={}, current recordedAt={}",
                        event.eventId(),
                        event.driverId(),
                        event.recordedAt(),
                        currentRecordedAt
                );

                return;
            }
        }

        // Only an accepted newer event may replace the Redis projection.
        Map<String, String> location = Map.of(
                "driverId", event.driverId().toString(),
                "latitude", event.latitude().toPlainString(),
                "longitude", event.longitude().toPlainString(),
                "recordedAt", event.recordedAt().toString(),
                "receivedAt", event.receivedAt().toString(),
                "eventId", event.eventId().toString()
        );

        redisTemplate.opsForHash().putAll(key, location);
        redisTemplate.expire(key, LOCATION_TTL);

        // Publish only after Redis contains the accepted latest location.
        // WebSocket delivery is best-effort; reconnecting clients recover from Redis.
        try {
            webSocketPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "Could not push location event {} to WebSocket clients",
                    event.eventId(),
                    exception
            );
        }

        String alertKey =
                STALE_ALERT_KEY_PREFIX + event.driverId() + ":stale";

        redisTemplate.delete(alertKey);
        redisTemplate.delete(alertKey + ":lock");

        log.debug(
                "Updated Redis latest location for driver {}",
                event.driverId()
        );
    }
}
