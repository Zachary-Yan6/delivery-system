package com.zachary.delivery_system.service;

import com.zachary.delivery_system.dto.Route.RoutePlanRequest;
import com.zachary.delivery_system.dto.Route.RoutePlanResponse;

public interface RoutePlanningService {
    RoutePlanResponse plan(RoutePlanRequest request);
}