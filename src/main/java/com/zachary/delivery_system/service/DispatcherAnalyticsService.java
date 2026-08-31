package com.zachary.delivery_system.service;

import com.zachary.delivery_system.dto.Location.DriverLocationActivityResponse;

import java.util.List;

public interface DispatcherAnalyticsService {

    List<DriverLocationActivityResponse> getLocationActivity(int minutes);
}