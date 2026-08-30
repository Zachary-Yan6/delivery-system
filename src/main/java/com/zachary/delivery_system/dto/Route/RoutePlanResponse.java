package com.zachary.delivery_system.dto.Route;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoutePlanResponse {
    private RouteOriginResponse origin;
    private List<RouteStopResponse> stops;
    private List<RoutePointResponse> geometry;
    private double distanceMeters;
    private double durationSeconds;
}