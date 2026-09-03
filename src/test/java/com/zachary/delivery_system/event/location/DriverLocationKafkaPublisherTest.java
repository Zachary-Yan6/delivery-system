package com.zachary.delivery_system.event.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationKafkaPublisherTest {

    @Mock
    private KafkaTemplate<
            String,
            DriverLocationReportedEvent
            > kafkaTemplate;

    @Test
    void publishUsesDriverIdAsKeyAndPreservesEventId() {
        UUID eventId = UUID.randomUUID();

        DriverLocationReportedEvent event =
                new DriverLocationReportedEvent(
                        eventId,
                        17L,
                        new BigDecimal("51.507400"),
                        new BigDecimal("-0.127800"),
                        Instant.parse("2026-08-31T10:00:00Z"),
                        Instant.parse("2026-08-31T10:00:05Z")
                );

        when(kafkaTemplate.send(
                "driver.location.reported.v1",
                "17",
                event
        )).thenReturn(CompletableFuture.completedFuture(null));

        DriverLocationKafkaPublisher publisher =
                new DriverLocationKafkaPublisher(
                        kafkaTemplate,
                        "driver.location.reported.v1"
                );

        publisher.publish(event);

        verify(kafkaTemplate).send(
                "driver.location.reported.v1",
                "17",
                event
        );
    }
}
