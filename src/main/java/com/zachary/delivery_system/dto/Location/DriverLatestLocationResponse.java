package com.zachary.delivery_system.dto.Location;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class DriverLatestLocationResponse {

    private Long driverId;
    private String driverName;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Date recordedAt;
    private Date receivedAt;
}