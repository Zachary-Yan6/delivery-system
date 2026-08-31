package com.zachary.delivery_system.dto.Location;

import lombok.Data;

import java.util.Date;

@Data
public class DriverLocationAlertResponse {

    private Long driverId;
    private String driverName;
    private String type;
    private String message;
    private Date lastReceivedAt;
    private Date raisedAt;
}