package com.zachary.delivery_system.event;

import com.zachary.delivery_system.entity.DriverLocationOutbox;
import com.zachary.delivery_system.event.location.DriverLocationKafkaPublisher;
import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.service.impl.DriverLocationOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverLocationOutboxPublisher {

    private final DriverLocationOutboxService outboxService;
    private final DriverLocationKafkaPublisher kafkaPublisher;

    @Value("${app.outbox.driver-location.batch-size:20}")
    private int batchSize;

    /**
     * call automatically every second
     */
    @Scheduled(
            fixedDelayString =
                    "${app.outbox.driver-location.fixed-delay-ms:1000}"
    )
    public void publishPendingEvents() {
        List<DriverLocationOutbox> outboxEvents =
                outboxService.claimBatch(batchSize);

        for (DriverLocationOutbox outbox : outboxEvents) {
            publishOne(outbox);
        }
    }

    private void publishOne(DriverLocationOutbox outbox) {
        try {
            DriverLocationReportedEvent event =
                    outboxService.deserialize(outbox);

            /**
             * blocking until kfaka send successfully, then outbox mark event published
             */
            kafkaPublisher.publish(event);
            outboxService.markPublished(outbox);
        } catch (Exception publishFailure) {
            log.warn(
                    "Could not publish outbox event {}",
                    outbox.getId(),
                    publishFailure
            );

            try {
                outboxService.scheduleRetry(
                        outbox,
                        publishFailure
                );
            } catch (Exception retryFailure) {
                log.error(
                        "Could not reschedule outbox event {}",
                        outbox.getId(),
                        retryFailure
                );
            }
        }
    }
}