package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.DriverLocationOutbox;
import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.mapper.DriverLocationOutboxMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocationOutboxServiceTest {

    @Mock
    private DriverLocationOutboxMapper outboxMapper;
    @Mock
    private ObjectMapper objectMapper;

    private DriverLocationOutboxService service;
    private DriverLocationReportedEvent event;

    @BeforeEach
    void setUp() {
        service = new DriverLocationOutboxService(outboxMapper, objectMapper);
        event = new DriverLocationReportedEvent(
                UUID.fromString("b2b2a42e-0d5b-4fb1-8f4d-3285587b6b71"),
                17L,
                new BigDecimal("51.507400"),
                new BigDecimal("-0.127800"),
                Instant.parse("2026-08-31T10:00:00Z"),
                Instant.parse("2026-08-31T10:00:05Z")
        );
    }

    @Test
    void savePendingSerializesAndInsertsTheEvent() throws Exception {
        when(objectMapper.writeValueAsString(event)).thenReturn("json");
        when(outboxMapper.insertPending(
                event.eventId(),
                "DRIVER_LOCATION_REPORTED_V1",
                17L,
                "json"
        )).thenReturn(1);

        service.savePending(event);

        verify(outboxMapper).insertPending(
                event.eventId(),
                "DRIVER_LOCATION_REPORTED_V1",
                17L,
                "json"
        );
    }

    @Test
    void savePendingRejectsAnUnexpectedInsertCount() throws Exception {
        when(objectMapper.writeValueAsString(event)).thenReturn("json");
        when(outboxMapper.insertPending(any(), any(), any(), any()))
                .thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.savePending(event)
        );

        assertTrue(exception.getMessage().contains("Could not create"));
    }

    @Test
    void savePendingWrapsSerializationFailure() throws Exception {
        JacksonException failure = mock(JacksonException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(failure);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.savePending(event)
        );

        assertSame(failure, exception.getCause());
        verifyNoInteractions(outboxMapper);
    }

    @Test
    void claimBatchValidatesBoundsAndUsesANewLockToken() {
        assertThrows(IllegalArgumentException.class,
                () -> service.claimBatch(0));
        assertThrows(IllegalArgumentException.class,
                () -> service.claimBatch(101));
        DriverLocationOutbox outbox = outbox();
        when(outboxMapper.claimBatch(eq(20), any(UUID.class)))
                .thenReturn(List.of(outbox));

        List<DriverLocationOutbox> result = service.claimBatch(20);

        assertEquals(List.of(outbox), result);
        ArgumentCaptor<UUID> tokenCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(outboxMapper).claimBatch(eq(20), tokenCaptor.capture());
        assertTrue(tokenCaptor.getValue().toString().length() > 10);
    }

    @Test
    void deserializeValidatesTypeAndParsesPayload() throws Exception {
        DriverLocationOutbox outbox = outbox();
        outbox.setEventType("DRIVER_LOCATION_REPORTED_V1");
        outbox.setPayload("json");
        when(objectMapper.readValue("json", DriverLocationReportedEvent.class))
                .thenReturn(event);

        assertSame(event, service.deserialize(outbox));

        outbox.setEventType("UNKNOWN");
        assertThrows(IllegalStateException.class,
                () -> service.deserialize(outbox));
    }

    @Test
    void deserializeWrapsMalformedJson() throws Exception {
        DriverLocationOutbox outbox = outbox();
        outbox.setEventType("DRIVER_LOCATION_REPORTED_V1");
        outbox.setPayload("bad-json");
        JacksonException failure = mock(JacksonException.class);
        when(objectMapper.readValue(
                "bad-json",
                DriverLocationReportedEvent.class
        )).thenThrow(failure);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.deserialize(outbox)
        );

        assertSame(failure, exception.getCause());
        assertTrue(exception.getMessage().contains(outbox.getId().toString()));
    }

    @Test
    void markPublishedRequiresOwnershipOfTheClaim() {
        DriverLocationOutbox outbox = outbox();
        when(outboxMapper.markPublished(outbox.getId(), outbox.getLockToken()))
                .thenReturn(1, 0);

        service.markPublished(outbox);

        assertThrows(
                IllegalStateException.class,
                () -> service.markPublished(outbox)
        );
    }

    @Test
    void scheduleRetryUsesFailureMessageAndChecksUpdateCount() {
        DriverLocationOutbox outbox = outbox();
        when(outboxMapper.scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                "Kafka down"
        )).thenReturn(1, 0);

        service.scheduleRetry(outbox, new RuntimeException("Kafka down"));

        assertThrows(
                IllegalStateException.class,
                () -> service.scheduleRetry(
                        outbox,
                        new RuntimeException("Kafka down")
                )
        );
    }

    @Test
    void scheduleRetryFallsBackToExceptionClassForBlankMessage() {
        DriverLocationOutbox outbox = outbox();
        when(outboxMapper.scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                "RuntimeException"
        )).thenReturn(1);

        service.scheduleRetry(outbox, new RuntimeException("  "));

        verify(outboxMapper).scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                "RuntimeException"
        );
    }

    @Test
    void scheduleRetryFallsBackToExceptionClassForNullMessage() {
        DriverLocationOutbox outbox = outbox();
        when(outboxMapper.scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                "RuntimeException"
        )).thenReturn(1);

        service.scheduleRetry(outbox, new RuntimeException());

        verify(outboxMapper).scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                "RuntimeException"
        );
    }

    private DriverLocationOutbox outbox() {
        DriverLocationOutbox outbox = new DriverLocationOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setLockToken(UUID.randomUUID());
        return outbox;
    }
}
