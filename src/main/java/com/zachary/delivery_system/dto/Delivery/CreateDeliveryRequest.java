package com.zachary.delivery_system.dto.Delivery;

import com.zachary.delivery_system.enums.DeliveryPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreateDeliveryRequest {

    /**
     * Optional while existing deliveries are being migrated. When present, it must
     * identify a CUSTOMER account.
     */
    private Long ownerId;

    @NotBlank(message = "customerName can not be empty!")
    private String customerName;

    private String customerPhone;

    @NotBlank(message = "address can not be empty!")
    private String address;

    @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90 and 90")
    private BigDecimal pickupLatitude;

    @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180 and 180")
    private BigDecimal pickupLongitude;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal destinationLatitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal destinationLongitude;

    private DeliveryPriority priority = DeliveryPriority.NORMAL;

    @DecimalMin(value = "0.01", message = "Package weight must be greater than zero")
    private BigDecimal packageWeightKg = BigDecimal.ONE;

    private Date timeWindowStart;

    private Date timeWindowEnd;

    @AssertTrue(message = "Pickup latitude and longitude must be provided together")
    public boolean isPickupLocationComplete() {
        return (pickupLatitude == null) == (pickupLongitude == null);
    }

    @AssertTrue(message = "Delivery time window must contain a start before its end")
    public boolean isTimeWindowValid() {
        if (timeWindowStart == null && timeWindowEnd == null) {
            return true;
        }

        return timeWindowStart != null
                && timeWindowEnd != null
                && timeWindowStart.before(timeWindowEnd);
    }

}
