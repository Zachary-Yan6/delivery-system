package com.zachary.delivery_system.projection.history;

import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverLocationHistoryConsumerTest {

    @Mock
    private DriverLocationEventsMapper driverLocationEventsMapper;

    @Test
    void storeLocationHistory_writesTheEventWithAnIdempotentInsert() {
        DriverLocationHistoryConsumer consumer =
                new DriverLocationHistoryConsumer(driverLocationEventsMapper);
        UUID eventId = UUID.fromString("b2b2a42e-0d5b-4fb1-8f4d-3285587b6b71");
        Instant recordedAt = Instant.parse("2026-08-31T10:00:00Z");
        Instant receivedAt = Instant.parse("2026-08-31T10:00:05Z");
        DriverLocationReportedEvent event = new DriverLocationReportedEvent(
                eventId,
                17L,
                new BigDecimal("51.507400"),
                new BigDecimal("-0.127800"),
                recordedAt,
                receivedAt
        );

        consumer.storeLocationHistory(event);

        verify(driverLocationEventsMapper).insertIgnore(
                receivedAt,
                eventId,
                17L,
                new BigDecimal("51.507400"),
                new BigDecimal("-0.127800"),
                recordedAt
        );
    }
}
