// RouteStopResponse.java
package com.zachary.delivery_system.dto.Route;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteStopResponse {
    private Long deliveryId;
    private String customerName;
    private String address;
    private int sequence;
}