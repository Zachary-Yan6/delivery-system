package com.zachary.delivery_system.service;

import com.zachary.delivery_system.dto.Location.DriverLocationAlertResponse;

import java.util.List;

public interface DispatcherAlertService {

    List<DriverLocationAlertResponse> getActiveAlerts();
}