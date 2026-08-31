package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.dto.Delivery.DeliveryEtaResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.service.DeliveryEtaService;
import com.zachary.delivery_system.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryEtaServiceImpl implements DeliveryEtaService {

    private final DeliveryService deliveryService;
    private final StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
            .build();

    @Value("${routing.osrm.base-url}")
    private String osrmBaseUrl;

    @Value("${app.eta.fixed-speed-kph}")
    private double fixedSpeedKph;

    @Override
    public DeliveryEtaResponse calculateEta(Long deliveryId) {
        Delivery delivery = deliveryService.getById(deliveryId);

        if (delivery == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Delivery not found: " + deliveryId
            );
        }

        if (delivery.getDriverId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ETA is unavailable because this delivery has no assigned driver."
            );
        }

        if (!"ASSIGNED".equals(delivery.getStatus())
                && !"IN_TRANSIT".equals(delivery.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ETA is available only for assigned or in-transit deliveries."
            );
        }

        if (delivery.getDestinationLatitude() == null
                || delivery.getDestinationLongitude() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ETA is unavailable because this delivery has no destination pin."
            );
        }

        if (fixedSpeedKph <= 0) {
            throw new IllegalStateException(
                    "app.eta.fixed-speed-kph must be greater than zero"
            );
        }

        Map<Object, Object> latestLocation = redisTemplate.opsForHash()
                .entries(
                        "tracking:driver:"
                                + delivery.getDriverId()
                                + ":latest"
                );

        if (latestLocation.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ETA is unavailable because the driver has not reported a current location."
            );
        }

        BigDecimal driverLatitude = new BigDecimal(
                valueOf(latestLocation, "latitude")
        );
        BigDecimal driverLongitude = new BigDecimal(
                valueOf(latestLocation, "longitude")
        );

        double distanceMeters = calculateRoadDistance(
                driverLatitude,
                driverLongitude,
                delivery.getDestinationLatitude(),
                delivery.getDestinationLongitude()
        );

        double speedMetersPerSecond = fixedSpeedKph * 1_000 / 3_600;

        long durationSeconds = (long) Math.ceil(
                distanceMeters / speedMetersPerSecond
        );

        Instant calculatedAt = Instant.now();
        Instant estimatedArrivalAt = calculatedAt.plusSeconds(durationSeconds);

        return new DeliveryEtaResponse(
                delivery.getId(),
                delivery.getDriverId(),
                distanceMeters,
                fixedSpeedKph,
                durationSeconds,
                estimatedArrivalAt,
                calculatedAt
        );
    }

    private double calculateRoadDistance(
            BigDecimal driverLatitude,
            BigDecimal driverLongitude,
            BigDecimal destinationLatitude,
            BigDecimal destinationLongitude
    ) {
        String coordinates =
                driverLongitude.toPlainString()
                        + ","
                        + driverLatitude.toPlainString()
                        + ";"
                        + destinationLongitude.toPlainString()
                        + ","
                        + destinationLatitude.toPlainString();

        URI osrmUri = URI.create(
                osrmBaseUrl
                        + "/route/v1/driving/"
                        + coordinates
                        + "?overview=false&steps=false"
        );

        JsonNode osrmResponse;

        try {
            osrmResponse = restClient.get()
                    .uri(osrmUri)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OSRM could not calculate a route for this driver and destination."
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The route service is temporarily unavailable."
            );
        }

        if (osrmResponse == null
                || !"Ok".equals(osrmResponse.path("code").asText())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OSRM could not find a road route for this delivery."
            );
        }

        return osrmResponse.path("routes").path(0).path("distance").asDouble();
    }

    private String valueOf(
            Map<Object, Object> location,
            String fieldName
    ) {
        Object value = location.get(fieldName);

        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "ETA is unavailable because the driver location is incomplete."
            );
        }

        return value.toString();
    }
}