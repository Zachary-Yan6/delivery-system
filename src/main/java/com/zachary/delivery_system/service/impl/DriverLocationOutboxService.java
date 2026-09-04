package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.DriverLocationOutbox;
import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.mapper.DriverLocationOutboxMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverLocationOutboxService {

    private static final String EVENT_TYPE =
            "DRIVER_LOCATION_REPORTED_V1";

    private final DriverLocationOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    /**
     * MANDATORY means this method must be called inside an existing transaction.
     * Therefore location and outbox are committed or rolled back together.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void savePending(DriverLocationReportedEvent event) {

        /**
         * convert Java object into JSON string
         */
        String payload = serialize(event);

        int insertedRows = outboxMapper.insertPending(
                event.eventId(),
                EVENT_TYPE,
                event.driverId(),
                payload
        );

        if (insertedRows != 1) {
            throw new IllegalStateException(
                    "Could not create driver-location outbox event"
            );
        }
    }

    @Transactional
    public List<DriverLocationOutbox> claimBatch(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Outbox batch size must be between 1 and 100"
            );
        }

        UUID lockToken = UUID.randomUUID();
        return outboxMapper.claimBatch(limit, lockToken);
    }

    public DriverLocationReportedEvent deserialize(
            DriverLocationOutbox outbox
    ) {
        if (!EVENT_TYPE.equals(outbox.getEventType())) {
            throw new IllegalStateException(
                    "Unsupported outbox event type: "
                            + outbox.getEventType()
            );
        }

        try {
            return objectMapper.readValue(
                    outbox.getPayload(),
                    DriverLocationReportedEvent.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not deserialize outbox event " + outbox.getId(),
                    exception
            );
        }
    }

    @Transactional
    public void markPublished(DriverLocationOutbox outbox) {
        int updatedRows = outboxMapper.markPublished(
                outbox.getId(),
                outbox.getLockToken()
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Outbox event is no longer owned by this publisher: "
                            + outbox.getId()
            );
        }
    }

    @Transactional
    public void scheduleRetry(
            DriverLocationOutbox outbox,
            Throwable failure
    ) {
        String message = failure.getMessage();

        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }

        int updatedRows = outboxMapper.scheduleRetry(
                outbox.getId(),
                outbox.getLockToken(),
                message
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Could not reschedule outbox event: " + outbox.getId()
            );
        }
    }

    private String serialize(DriverLocationReportedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize driver-location event",
                    exception
            );
        }
    }
}