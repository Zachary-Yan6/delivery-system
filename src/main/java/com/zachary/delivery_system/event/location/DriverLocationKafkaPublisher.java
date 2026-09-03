package com.zachary.delivery_system.event.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DriverLocationKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DriverLocationKafkaPublisher.class
            );

    private final KafkaTemplate<
            String,
            DriverLocationReportedEvent
            > kafkaTemplate;

    private final String topicName;

    public DriverLocationKafkaPublisher(
            KafkaTemplate<String, DriverLocationReportedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.driver-location-reported}")
            String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    /**
     * Wait for Kafka acknowledgement before marking an outbox event published.
     */
    public void publish(DriverLocationReportedEvent event) {
        try {
            kafkaTemplate.send(
                    topicName,
                    event.driverId().toString(),
                    event
            ).get(10, TimeUnit.SECONDS);

            log.debug(
                    "Published location event {} for driver {}",
                    event.eventId(),
                    event.driverId()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Kafka publishing was interrupted",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Could not publish driver-location event to Kafka",
                    exception
            );
        }
    }
}