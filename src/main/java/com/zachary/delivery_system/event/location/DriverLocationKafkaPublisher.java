package com.zachary.delivery_system.event.location;

import com.zachary.delivery_system.entity.DriverLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DriverLocationKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(DriverLocationKafkaPublisher.class);

    private final KafkaTemplate<String, DriverLocationReportedEvent> kafkaTemplate;
    private final String topicName;

    public DriverLocationKafkaPublisher(
            KafkaTemplate<String, DriverLocationReportedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.driver-location-reported}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(DriverLocation location) {
        DriverLocationReportedEvent event =
                new DriverLocationReportedEvent(
                        UUID.randomUUID(),
                        location.getDriverId(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getRecordedAt().toInstant(),
                        location.getReceivedAt().toInstant()
                );

        kafkaTemplate.send(
                topicName,
                location.getDriverId().toString(),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Could not publish location event for driver {}",
                        location.getDriverId(),
                        exception
                );
            }
        });
    }
}