package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Delivery.DeliveryEtaResponse;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationAlertResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanResponse;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.service.DeliveryEtaService;
import com.zachary.delivery_system.service.DispatcherAlertService;
import com.zachary.delivery_system.service.DispatcherAnalyticsService;
import com.zachary.delivery_system.service.DriverLocationService;
import com.zachary.delivery_system.service.RoutePlanningService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForwardingControllersTest {

    @Test
    void deliveryEtaControllerDelegatesToTheService() {
        DeliveryEtaService service = mock(DeliveryEtaService.class);
        DeliveryEtaResponse expected = mock(DeliveryEtaResponse.class);
        when(service.calculateEta(12L)).thenReturn(expected);

        DeliveryEtaResponse result =
                new DeliveryEtaController(service).calculateEta(12L);

        assertSame(expected, result);
    }

    @Test
    void dispatcherAlertControllerDelegatesToTheService() {
        DispatcherAlertService service = mock(DispatcherAlertService.class);
        List<DriverLocationAlertResponse> expected =
                List.of(new DriverLocationAlertResponse());
        when(service.getActiveAlerts()).thenReturn(expected);

        List<DriverLocationAlertResponse> result =
                new DispatcherAlertController(service).getActiveAlerts();

        assertSame(expected, result);
    }

    @Test
    void dispatcherAnalyticsControllerForwardsTheWindow() {
        DispatcherAnalyticsService service =
                mock(DispatcherAnalyticsService.class);
        List<DriverLocationActivityResponse> expected = List.of();
        when(service.getLocationActivity(120)).thenReturn(expected);

        List<DriverLocationActivityResponse> result =
                new DispatcherAnalyticsController(service)
                        .getLocationActivity(120);

        assertSame(expected, result);
    }

    @Test
    void dispatcherLocationControllerDelegatesToTheService() {
        DriverLocationService service = mock(DriverLocationService.class);
        List<DriverLatestLocationResponse> expected =
                List.of(new DriverLatestLocationResponse());
        when(service.getLatestLocations()).thenReturn(expected);

        List<DriverLatestLocationResponse> result =
                new DispatcherLocationController(service)
                        .getLatestDriverLocations();

        assertSame(expected, result);
    }

    @Test
    void driverLocationControllerForwardsTheAuthenticatedUserAndRequest() {
        DriverLocationService service = mock(DriverLocationService.class);
        DriverLocationRequest request = new DriverLocationRequest();
        AppUser user = new AppUser();
        DriverLocation expected = new DriverLocation();
        when(service.recordLocation(user, request)).thenReturn(expected);

        DriverLocation result = new DriverLocationController(service)
                .postLocation(user, request);

        assertSame(expected, result);
        verify(service).recordLocation(user, request);
    }

    @Test
    void routePlanningControllerForwardsTheRequest() {
        RoutePlanningService service = mock(RoutePlanningService.class);
        RoutePlanRequest request = new RoutePlanRequest();
        RoutePlanResponse expected = mock(RoutePlanResponse.class);
        when(service.plan(request)).thenReturn(expected);

        RoutePlanResponse result =
                new RoutePlanningController(service).planRoute(request);

        assertSame(expected, result);
    }
}
