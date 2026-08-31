package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import com.zachary.delivery_system.service.DispatcherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DispatcherAnalyticsServiceImpl
        implements DispatcherAnalyticsService {

    private final DriverLocationEventsMapper driverLocationEventsMapper;

    @Override
    public List<DriverLocationActivityResponse> getLocationActivity(int minutes) {
        if (minutes < 1 || minutes > 1_440) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minutes must be between 1 and 1440"
            );
        }

        Instant since = Instant.now().minus(Duration.ofMinutes(minutes));

        return driverLocationEventsMapper.selectActivitySince(since);
    }
}