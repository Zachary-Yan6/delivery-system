package com.zachary.delivery_system.service.impl;

import tools.jackson.databind.JsonNode;
import com.zachary.delivery_system.dto.Route.RoutePlanRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanResponse;
import com.zachary.delivery_system.dto.Route.RoutePointResponse;
import com.zachary.delivery_system.dto.Route.RouteStopResponse;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.service.RoutePlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zachary.delivery_system.dto.Route.RouteOriginResponse;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.mapper.DriverLocationMapper;
import com.zachary.delivery_system.service.DriverService;
@Service
@RequiredArgsConstructor
public class RoutePlanningServiceImpl implements RoutePlanningService {

    private final DeliveryService deliveryService;
    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "identity")
            .build();

    private final DriverService driverService;
    private final DriverLocationMapper driverLocationMapper;
    @Value("${routing.osrm.base-url}")
    private String osrmBaseUrl;

    @Override
    public RoutePlanResponse plan(RoutePlanRequest request) {

        // receive the deliveries id from front end
        List<Long> deliveryIds = request.getDeliveryIds();

        Driver driver = driverService.getById(request.getDriverId());

        if (driver == null || !Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select an active driver for this route."
            );
        }

        DriverLocation originLocation = driverLocationMapper.selectOne(
                new LambdaQueryWrapper<DriverLocation>()
                        .eq(DriverLocation::getDriverId, driver.getId())
                        .orderByDesc(DriverLocation::getReceivedAt)
                        .last("LIMIT 1")
        );

        if (originLocation == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This driver has not reported a location yet."
            );
        }


        // remove duplicates
        if (new HashSet<>(deliveryIds).size() != deliveryIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A delivery can only appear once in a route."
            );
        }

        //
        Map<Long, Delivery> deliveriesById = deliveryService.listByIds(deliveryIds)
                .stream()
                .collect(Collectors.toMap(Delivery::getId, Function.identity()));


        if (deliveriesById.size() != deliveryIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more selected deliveries do not exist."
            );
        }

        // list all existing deliveries
        List<Delivery> orderedDeliveries = deliveryIds.stream()
                .map(deliveriesById::get)
                .toList();


        for (Delivery delivery : orderedDeliveries) {
            if (delivery.getDestinationLatitude() == null
                    || delivery.getDestinationLongitude() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Delivery #" + delivery.getId() + " does not have a destination pin."
                );
            }

            if ("DELIVERED".equals(delivery.getStatus())
                    || "FAILED".equals(delivery.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Completed deliveries cannot be added to a route."
                );
            }
        }

        String deliveryCoordinates = orderedDeliveries.stream()
                .map(delivery ->
                        delivery.getDestinationLongitude().toPlainString()
                                + ","
                                + delivery.getDestinationLatitude().toPlainString()
                )
                .collect(Collectors.joining(";"));

        String coordinates =
                originLocation.getLongitude().toPlainString()
                        + ","
                        + originLocation.getLatitude().toPlainString()
                        + ";"
                        + deliveryCoordinates;

        URI osrmUri = URI.create(
                osrmBaseUrl
                        + "/route/v1/driving/"
                        + coordinates
                        + "?overview=full&geometries=geojson&steps=false"
        );

        JsonNode osrmResponse;

        try {
            osrmResponse = restClient
                    .get()
                    .uri(osrmUri)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OSRM could not calculate a road route for these delivery locations. "
                            + "Make sure every pin is close to a road and all stops are reachable by car."
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The route service is temporarily unavailable."
            );
        }

        if (osrmResponse == null || !"Ok".equals(osrmResponse.path("code").asText())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OSRM could not find a road route for these locations."
            );
        }

        JsonNode route = osrmResponse.path("routes").path(0);

        List<RoutePointResponse> geometry = new ArrayList<>();
        for (JsonNode point : route.path("geometry").path("coordinates")) {
            geometry.add(new RoutePointResponse(
                    point.get(1).asDouble(),
                    point.get(0).asDouble()
            ));
        }

        List<RouteStopResponse> stops = new ArrayList<>();
        for (int index = 0; index < orderedDeliveries.size(); index++) {
            Delivery delivery = orderedDeliveries.get(index);

            stops.add(new RouteStopResponse(
                    delivery.getId(),
                    delivery.getCustomerName(),
                    delivery.getAddress(),
                    index + 1
            ));
        }

        return new RoutePlanResponse(
                new RouteOriginResponse(
                        driver.getId(),
                        driver.getFullName(),
                        originLocation.getLatitude().doubleValue(),
                        originLocation.getLongitude().doubleValue()
                ),
                stops,
                geometry,
                route.path("distance").asDouble(),
                route.path("duration").asDouble()
        );
    }
}
