package com.zachary.delivery_system.dto.Location;

import lombok.Data;

import java.util.Date;

@Data
public class DriverLocationActivityResponse {

    private Long driverId;
    private String driverName;
    private Long locationCount;
    private Date lastReceivedAt;
}