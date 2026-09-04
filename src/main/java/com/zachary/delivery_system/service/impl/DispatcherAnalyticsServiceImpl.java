package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import com.zachary.delivery_system.service.DispatcherAnalyticsService;
import com.zachary.delivery_system.exception.AnalyticsWindowInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatcherAnalyticsServiceImpl
        implements DispatcherAnalyticsService {

    private final DriverLocationEventsMapper driverLocationEventsMapper;

    @Override
    public List<DriverLocationActivityResponse> getLocationActivity(
            int minutes
    ) {
        if (minutes < 1 || minutes > 1_440) {
            throw new AnalyticsWindowInvalidException();
        }

        Instant since = Instant.now()
                .minus(Duration.ofMinutes(minutes));

        Instant onlineCutoff = Instant.now()
                .minus(Duration.ofSeconds(30));

        return driverLocationEventsMapper
                .selectActivitySince(since)
                .stream()
                .map(projection ->
                        new DriverLocationActivityResponse(
                                projection.getDriverId(),
                                projection.getDriverName(),
                                projection.getLocationCount(),
                                projection.getLastReceivedAt(),
                                projection.getLastReceivedAt()
                                        .isAfter(onlineCutoff)
                        )
                )
                .toList();
    }
}
