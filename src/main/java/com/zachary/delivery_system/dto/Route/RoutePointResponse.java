// RoutePointResponse.java
package com.zachary.delivery_system.dto.Route;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoutePointResponse {
    private double latitude;
    private double longitude;
}