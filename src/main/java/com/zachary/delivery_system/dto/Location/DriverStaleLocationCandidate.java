package com.zachary.delivery_system.dto.Location;

import lombok.Data;

import java.util.Date;

@Data
public class DriverStaleLocationCandidate {

    private Long driverId;
    private String driverName;
    private Date lastReceivedAt;
}