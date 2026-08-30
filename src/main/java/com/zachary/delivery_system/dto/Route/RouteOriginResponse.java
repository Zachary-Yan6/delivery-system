package com.zachary.delivery_system.dto.Route;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteOriginResponse {
    private Long driverId;
    private String driverName;
    private double latitude;
    private double longitude;
}