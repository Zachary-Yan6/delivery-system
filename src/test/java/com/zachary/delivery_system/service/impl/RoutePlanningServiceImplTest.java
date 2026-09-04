package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zachary.delivery_system.dto.Route.RoutePlanRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.RouteDeliveryNotFoundException;
import com.zachary.delivery_system.exception.RoutePlanningException;
import com.zachary.delivery_system.exception.RouteServiceUnavailableException;
import com.zachary.delivery_system.mapper.DriverLocationMapper;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class RoutePlanningServiceImplTest {

    @Mock
    private DeliveryService deliveryService;
    @Mock
    private DriverService driverService;
    @Mock
    private DriverLocationMapper driverLocationMapper;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;
    @Mock
    private RestClient.RequestHeadersSpec headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RoutePlanningServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoutePlanningServiceImpl(
                deliveryService,
                driverService,
                driverLocationMapper
        );
        ReflectionTestUtils.setField(service, "restClient", restClient);
        ReflectionTestUtils.setField(service, "osrmBaseUrl", "http://osrm.test");
    }

    @Test
    void planBuildsOrderedStopsGeometryAndDriverOrigin() throws Exception {
        RoutePlanRequest request = request(8L, 5L, 6L);
        Driver driver = activeDriver();
        DriverLocation origin = origin();
        Delivery first = delivery(5L, "First", "A", "51.60", "-0.20");
        Delivery second = delivery(6L, "Second", "B", "51.70", "-0.30");
        when(driverService.getById(8L)).thenReturn(driver);
        when(driverLocationMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(origin);
        // Return the opposite order to prove that request order is preserved.
        when(deliveryService.listByIds(List.of(5L, 6L)))
                .thenReturn(List.of(second, first));
        JsonNode response = new ObjectMapper().readTree("""
                {
                  "code": "Ok",
                  "routes": [{
                    "distance": 1500.5,
                    "duration": 300.0,
                    "geometry": {
                      "coordinates": [[-0.1, 51.5], [-0.2, 51.6]]
                    }
                  }]
                }
                """);
        stubOsrmResponse(response);

        RoutePlanResponse result = service.plan(request);

        assertEquals(8L, result.getOrigin().getDriverId());
        assertEquals("Driver Eight", result.getOrigin().getDriverName());
        assertEquals(51.5, result.getOrigin().getLatitude());
        assertEquals(-0.1, result.getOrigin().getLongitude());
        assertEquals(List.of(5L, 6L), result.getStops().stream()
                .map(stop -> stop.getDeliveryId()).toList());
        assertEquals(1, result.getStops().get(0).getSequence());
        assertEquals(2, result.getStops().get(1).getSequence());
        assertEquals(2, result.getGeometry().size());
        assertEquals(51.6, result.getGeometry().get(1).getLatitude());
        assertEquals(-0.2, result.getGeometry().get(1).getLongitude());
        assertEquals(1500.5, result.getDistanceMeters());
        assertEquals(300.0, result.getDurationSeconds());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestSpec).uri(uriCaptor.capture());
        assertTrue(uriCaptor.getValue().toString().contains(
                "-0.1,51.5;-0.20,51.60;-0.30,51.70"
        ));
    }

    @Test
    void planRejectsMissingAndInactiveDrivers() {
        RoutePlanRequest request = request(8L, 5L, 6L);
        when(driverService.getById(8L))
                .thenAnswer(invocation -> null);
        assertRouteCode("ROUTE_DRIVER_UNAVAILABLE", () -> service.plan(request));

        Driver inactive = activeDriver();
        inactive.setActive(false);
        when(driverService.getById(8L)).thenReturn(inactive);
        assertRouteCode("ROUTE_DRIVER_UNAVAILABLE", () -> service.plan(request));
    }

    @Test
    void planRequiresAReportedDriverOrigin() {
        when(driverService.getById(8L)).thenReturn(activeDriver());
        when(driverLocationMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> null);

        assertRouteCode(
                "ROUTE_ORIGIN_MISSING",
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    @Test
    void planRejectsDuplicateDeliveryIds() {
        stubDriverAndOrigin();

        assertRouteCode(
                "ROUTE_DUPLICATE_DELIVERY",
                () -> service.plan(request(8L, 5L, 5L))
        );
        verify(deliveryService, never()).listByIds(any());
    }

    @Test
    void planRejectsUnknownDeliveryIds() {
        stubDriverAndOrigin();
        when(deliveryService.listByIds(List.of(5L, 6L)))
                .thenReturn(List.of(delivery(5L, "First", "A", "51", "-1")));

        assertThrows(
                RouteDeliveryNotFoundException.class,
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    @Test
    void planRejectsMissingDestinationAndTerminalDelivery() {
        stubDriverAndOrigin();
        Delivery missing = delivery(5L, "First", "A", "51", "-1");
        missing.setDestinationLongitude(null);
        when(deliveryService.listByIds(List.of(5L, 6L))).thenReturn(List.of(
                missing,
                delivery(6L, "Second", "B", "52", "-2")
        ));
        assertRouteCode(
                "ROUTE_DESTINATION_MISSING",
                () -> service.plan(request(8L, 5L, 6L))
        );

        Delivery terminal = delivery(5L, "First", "A", "51", "-1");
        terminal.setStatus(DeliveryStatus.DELIVERED);
        when(deliveryService.listByIds(List.of(5L, 6L))).thenReturn(List.of(
                terminal,
                delivery(6L, "Second", "B", "52", "-2")
        ));
        assertRouteCode(
                "ROUTE_DELIVERY_TERMINAL",
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    @Test
    void planRejectsNullDeliveryStatus() {
        stubDriverAndOrigin();
        Delivery delivery = delivery(5L, "First", "A", "51", "-1");
        delivery.setStatus(null);
        when(deliveryService.listByIds(List.of(5L, 6L))).thenReturn(List.of(
                delivery,
                delivery(6L, "Second", "B", "52", "-2")
        ));

        assertRouteCode(
                "ROUTE_DELIVERY_TERMINAL",
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    @Test
    void planMapsOsrmHttpAndConnectionFailures() {
        stubValidRouteInput();
        stubOsrmFailure(httpFailure());
        assertRouteCode(
                "ROUTE_CALCULATION_FAILED",
                () -> service.plan(request(8L, 5L, 6L))
        );

        stubOsrmFailure(new RestClientException("offline"));
        assertThrows(
                RouteServiceUnavailableException.class,
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    @Test
    void planRejectsNullAndNonOkOsrmResponses() {
        stubValidRouteInput();
        stubOsrmResponse(null);
        assertRouteCode(
                "ROUTE_NOT_FOUND",
                () -> service.plan(request(8L, 5L, 6L))
        );

        JsonNode response = mock(JsonNode.class);
        JsonNode code = mock(JsonNode.class);
        when(response.path("code")).thenReturn(code);
        when(code.asText()).thenReturn("NoRoute");
        stubOsrmResponse(response);
        assertRouteCode(
                "ROUTE_NOT_FOUND",
                () -> service.plan(request(8L, 5L, 6L))
        );
    }

    private void stubDriverAndOrigin() {
        when(driverService.getById(8L)).thenReturn(activeDriver());
        when(driverLocationMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(origin());
    }

    private void stubValidRouteInput() {
        stubDriverAndOrigin();
        when(deliveryService.listByIds(List.of(5L, 6L))).thenReturn(List.of(
                delivery(5L, "First", "A", "51", "-1"),
                delivery(6L, "Second", "B", "52", "-2")
        ));
    }

    private void stubOsrmResponse(JsonNode response) {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        doAnswer(invocation -> response)
                .when(responseSpec).body(JsonNode.class);
    }

    private void stubOsrmFailure(RestClientException failure) {
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(any(URI.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        doAnswer(invocation -> {
            throw failure;
        }).when(responseSpec).body(JsonNode.class);
    }

    private RestClientResponseException httpFailure() {
        return new RestClientResponseException(
                "OSRM failure",
                500,
                "Server Error",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );
    }

    private void assertRouteCode(String code, Runnable invocation) {
        RoutePlanningException exception = assertThrows(
                RoutePlanningException.class,
                invocation::run
        );
        assertEquals(code, exception.getCode());
    }

    private RoutePlanRequest request(Long driverId, Long... deliveryIds) {
        RoutePlanRequest request = new RoutePlanRequest();
        request.setDriverId(driverId);
        request.setDeliveryIds(List.of(deliveryIds));
        return request;
    }

    private Driver activeDriver() {
        Driver driver = new Driver();
        driver.setId(8L);
        driver.setFullName("Driver Eight");
        driver.setActive(true);
        return driver;
    }

    private DriverLocation origin() {
        DriverLocation location = new DriverLocation();
        location.setDriverId(8L);
        location.setLatitude(new BigDecimal("51.5"));
        location.setLongitude(new BigDecimal("-0.1"));
        return location;
    }

    private Delivery delivery(
            Long id,
            String customer,
            String address,
            String latitude,
            String longitude
    ) {
        Delivery delivery = new Delivery();
        delivery.setId(id);
        delivery.setCustomerName(customer);
        delivery.setAddress(address);
        delivery.setDestinationLatitude(new BigDecimal(latitude));
        delivery.setDestinationLongitude(new BigDecimal(longitude));
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        return delivery;
    }
}
