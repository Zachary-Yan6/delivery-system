package com.zachary.delivery_system.projection.history;

import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationHistoryConsumer {

    private final DriverLocationEventsMapper driverLocationEventsMapper;

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topics.driver-location-reported}",
            groupId = "${app.kafka.groups.history-projection}"
    )
    public void storeLocationHistory(DriverLocationReportedEvent event) {
        int insertedRows = driverLocationEventsMapper.insertIgnore(
                event.receivedAt(),
                event.eventId(),
                event.driverId(),
                event.latitude(),
                event.longitude(),
                event.recordedAt()
        );

        log.debug(
                "Stored {} location-history row(s) for driver {}",
                insertedRows,
                event.driverId()
        );
    }
}