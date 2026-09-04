package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Delivery.DeliveryEtaResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.enums.DeliveryStatus;
import com.zachary.delivery_system.exception.DeliveryDestinationMissingException;
import com.zachary.delivery_system.exception.DeliveryEtaUnavailableException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.exception.RoutePlanningException;
import com.zachary.delivery_system.exception.RouteServiceUnavailableException;
import com.zachary.delivery_system.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class DeliveryEtaServiceImplTest {

    @Mock
    private DeliveryService deliveryService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;
    @Mock
    private RestClient.RequestHeadersSpec headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private DeliveryEtaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeliveryEtaServiceImpl(deliveryService, redisTemplate);
        ReflectionTestUtils.setField(service, "restClient", restClient);
        ReflectionTestUtils.setField(service, "osrmBaseUrl", "http://osrm.test");
        ReflectionTestUtils.setField(service, "fixedSpeedKph", 36.0);
    }

    @Test
    void calculateEtaUsesRoadDistanceAndFixedSpeed() {
        Delivery delivery = activeDelivery();
        when(deliveryService.getById(5L)).thenReturn(delivery);
        stubRedisLocation(Map.of(
                "latitude", "51.500000",
                "longitude", "-0.100000"
        ));
        stubOsrmResponse(okResponse(1_000.0));

        DeliveryEtaResponse result = service.calculateEta(5L);

        assertEquals(5L, result.getDeliveryId());
        assertEquals(8L, result.getDriverId());
        assertEquals(1_000.0, result.getDistanceMeters());
        assertEquals(36.0, result.getFixedSpeedKph());
        assertEquals(100L, result.getDurationSeconds());
        assertEquals(
                100L,
                result.getEstimatedArrivalAt()
                        .getEpochSecond()
                        - result.getCalculatedAt().getEpochSecond()
        );
        verify(requestSpec).uri(any(URI.class));
    }

    @Test
    void calculateEtaRejectsUnknownDelivery() {
        when(deliveryService.getById(5L))
                .thenAnswer(invocation -> null);

        assertThrows(DeliveryNotFoundException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaRejectsDeliveryWithoutDriver() {
        Delivery delivery = activeDelivery();
        delivery.setDriverId(null);
        when(deliveryService.getById(5L)).thenReturn(delivery);

        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaRejectsNullOrInactiveDeliveryStatus() {
        Delivery delivery = activeDelivery();
        delivery.setStatus(null);
        when(deliveryService.getById(5L)).thenReturn(delivery);
        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));

        delivery.setStatus(DeliveryStatus.DELIVERED);
        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaRejectsMissingDestinationCoordinates() {
        Delivery delivery = activeDelivery();
        delivery.setDestinationLatitude(null);
        when(deliveryService.getById(5L)).thenReturn(delivery);

        assertThrows(DeliveryDestinationMissingException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaRejectsNonPositiveConfiguredSpeed() {
        when(deliveryService.getById(5L)).thenReturn(activeDelivery());
        ReflectionTestUtils.setField(service, "fixedSpeedKph", 0.0);

        assertThrows(IllegalStateException.class,
                () -> service.calculateEta(5L));
        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    void calculateEtaRequiresAReportedCompleteDriverLocation() {
        when(deliveryService.getById(5L)).thenReturn(activeDelivery());
        stubRedisLocation(Map.of());
        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));

        stubRedisLocation(Map.of("longitude", "-0.1"));
        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));

        stubRedisLocation(Map.of("latitude", "51.5"));
        assertThrows(DeliveryEtaUnavailableException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaMapsOsrmHttpAndConnectionFailures() {
        when(deliveryService.getById(5L)).thenReturn(activeDelivery());
        stubRedisLocation(Map.of(
                "latitude", "51.5",
                "longitude", "-0.1"
        ));
        stubOsrmFailure(httpFailure());
        assertThrows(RoutePlanningException.class,
                () -> service.calculateEta(5L));

        stubOsrmFailure(new RestClientException("offline"));
        assertThrows(RouteServiceUnavailableException.class,
                () -> service.calculateEta(5L));
    }

    @Test
    void calculateEtaRejectsNullAndNonOkOsrmResponses() {
        when(deliveryService.getById(5L)).thenReturn(activeDelivery());
        stubRedisLocation(Map.of(
                "latitude", "51.5",
                "longitude", "-0.1"
        ));
        stubOsrmResponse(null);
        RoutePlanningException nullResponse = assertThrows(
                RoutePlanningException.class,
                () -> service.calculateEta(5L)
        );
        assertEquals("ROUTE_NOT_FOUND", nullResponse.getCode());

        JsonNode nonOk = mock(JsonNode.class);
        JsonNode code = mock(JsonNode.class);
        when(nonOk.path("code")).thenReturn(code);
        when(code.asText()).thenReturn("NoRoute");
        stubOsrmResponse(nonOk);
        assertThrows(RoutePlanningException.class,
                () -> service.calculateEta(5L));
    }

    private void stubRedisLocation(Map<Object, Object> location) {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("tracking:driver:8:latest"))
                .thenReturn(location);
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

    private JsonNode okResponse(double distance) {
        JsonNode root = mock(JsonNode.class);
        JsonNode code = mock(JsonNode.class);
        JsonNode routes = mock(JsonNode.class);
        JsonNode route = mock(JsonNode.class);
        JsonNode distanceNode = mock(JsonNode.class);
        when(root.path("code")).thenReturn(code);
        when(code.asText()).thenReturn("Ok");
        when(root.path("routes")).thenReturn(routes);
        when(routes.path(0)).thenReturn(route);
        when(route.path("distance")).thenReturn(distanceNode);
        when(distanceNode.asDouble()).thenReturn(distance);
        return root;
    }

    private Delivery activeDelivery() {
        Delivery delivery = new Delivery();
        delivery.setId(5L);
        delivery.setDriverId(8L);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        delivery.setDestinationLatitude(new BigDecimal("51.600000"));
        delivery.setDestinationLongitude(new BigDecimal("-0.200000"));
        return delivery;
    }
}
