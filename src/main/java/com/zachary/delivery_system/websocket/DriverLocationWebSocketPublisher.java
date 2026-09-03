package com.zachary.delivery_system.websocket;

import com.zachary.delivery_system.dto.Location.DriverLocationUpdateMessage;
import com.zachary.delivery_system.event.location.DriverLocationReportedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DriverLocationWebSocketPublisher {

    private final DriverLocationWebSocketHandler webSocketHandler;

    public void publish(DriverLocationReportedEvent event) {
        DriverLocationUpdateMessage message =
                new DriverLocationUpdateMessage(
                        event.driverId(),
                        event.latitude(),
                        event.longitude(),
                        event.recordedAt(),
                        event.receivedAt()
                );

        // All authenticated dispatcher dashboards receive the update.
        webSocketHandler.broadcastLocation(message);
    }
}
