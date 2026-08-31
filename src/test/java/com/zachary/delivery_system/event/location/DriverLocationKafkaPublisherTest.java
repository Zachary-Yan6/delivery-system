package com.zachary.delivery_system.event.location;

import com.zachary.delivery_system.entity.DriverLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, DriverLocationReportedEvent> kafkaTemplate;

    @Captor
    private ArgumentCaptor<DriverLocationReportedEvent> eventCaptor;

    @Test
    void publish_sendsAVersionedLocationEventUsingTheDriverIdAsKey() {
        DriverLocation location = new DriverLocation();
        location.setDriverId(17L);
        location.setLatitude(new BigDecimal("51.507400"));
        location.setLongitude(new BigDecimal("-0.127800"));
        location.setRecordedAt(Date.from(Instant.parse("2026-08-31T10:00:00Z")));
        location.setReceivedAt(Date.from(Instant.parse("2026-08-31T10:00:05Z")));

        when(kafkaTemplate.send(eq("driver.location.reported.v1"), eq("17"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        DriverLocationKafkaPublisher publisher = new DriverLocationKafkaPublisher(
                kafkaTemplate,
                "driver.location.reported.v1"
        );

        publisher.publish(location);

        verify(kafkaTemplate).send(
                eq("driver.location.reported.v1"),
                eq("17"),
                eventCaptor.capture()
        );

        DriverLocationReportedEvent event = eventCaptor.getValue();
        assertNotNull(event.eventId());
        assertEquals(17L, event.driverId());
        assertEquals(new BigDecimal("51.507400"), event.latitude());
        assertEquals(new BigDecimal("-0.127800"), event.longitude());
        assertEquals(Instant.parse("2026-08-31T10:00:00Z"), event.recordedAt());
        assertEquals(Instant.parse("2026-08-31T10:00:05Z"), event.receivedAt());
    }
}
