package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatcherAnalyticsServiceImplTest {

    @Mock
    private DriverLocationEventsMapper driverLocationEventsMapper;

    @Test
    void getLocationActivity_queriesOnlyTheRequestedTimeWindow() {
        List<DriverLocationActivityResponse> expected = List.of(
                new DriverLocationActivityResponse()
        );
        when(driverLocationEventsMapper.selectActivitySince(any(Instant.class)))
                .thenReturn(expected);
        DispatcherAnalyticsServiceImpl service =
                new DispatcherAnalyticsServiceImpl(driverLocationEventsMapper);
        Instant before = Instant.now().minusSeconds(15 * 60 + 1);

        List<DriverLocationActivityResponse> result =
                service.getLocationActivity(15);

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(driverLocationEventsMapper).selectActivitySince(sinceCaptor.capture());
        assertEquals(expected, result);
        assertEquals(true, sinceCaptor.getValue().isAfter(before));
        assertEquals(true, sinceCaptor.getValue().isBefore(Instant.now().minusSeconds(15 * 60 - 1)));
    }

    @Test
    void getLocationActivity_rejectsAnUnsupportedTimeWindow() {
        DispatcherAnalyticsServiceImpl service =
                new DispatcherAnalyticsServiceImpl(driverLocationEventsMapper);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getLocationActivity(0)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Minutes must be between 1 and 1440", exception.getReason());
        verifyNoInteractions(driverLocationEventsMapper);
    }
}
