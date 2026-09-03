package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.exception.AnalyticsWindowInvalidException;
import com.zachary.delivery_system.mapper.DriverLocationEventsMapper;
import com.zachary.delivery_system.projection.analytics.DriverLocationActivityProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        Instant lastReceivedAt = Instant.now();
        DriverLocationActivityProjection projection =
                new DriverLocationActivityProjection();
        projection.setDriverId(7L);
        projection.setDriverName("driver7");
        projection.setLocationCount(12L);
        projection.setLastReceivedAt(lastReceivedAt);

        List<DriverLocationActivityResponse> expected = List.of(
                new DriverLocationActivityResponse(
                        7L,
                        "driver7",
                        12L,
                        lastReceivedAt,
                        true
                )
        );
        when(driverLocationEventsMapper.selectActivitySince(any(Instant.class)))
                .thenReturn(List.of(projection));

        DispatcherAnalyticsServiceImpl service =
                new DispatcherAnalyticsServiceImpl(driverLocationEventsMapper);
        Instant before = Instant.now().minusSeconds(15 * 60 + 1);

        List<DriverLocationActivityResponse> result =
                service.getLocationActivity(15);

        // capture argument passed to mock method
        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);

        // to check if driverLocationEventsMapper calls selectActivitySince in this test
        verify(driverLocationEventsMapper).selectActivitySince(sinceCaptor.capture());
        assertEquals(expected, result);
        assertEquals(true, sinceCaptor.getValue().isAfter(before));
        assertEquals(true, sinceCaptor.getValue().isBefore(Instant.now().minusSeconds(15 * 60 - 1)));
    }

    @Test
    void getLocationActivity_rejectsAnUnsupportedTimeWindow() {
        DispatcherAnalyticsServiceImpl service =
                new DispatcherAnalyticsServiceImpl(driverLocationEventsMapper);

        AnalyticsWindowInvalidException exception = assertThrows(
                AnalyticsWindowInvalidException.class,
                () -> service.getLocationActivity(0)
        );

        assertEquals("ANALYTICS_WINDOW_INVALID", exception.getCode());
        assertEquals("Minutes must be between 1 and 1440", exception.getMessage());
        // to check if there is no call on driverLocationEventsMapper in this test
        verifyNoInteractions(driverLocationEventsMapper);
    }
}
