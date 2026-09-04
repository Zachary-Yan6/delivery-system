package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zachary.delivery_system.enums.DeliveryPriority;
import com.zachary.delivery_system.enums.DeliveryStatus;
import lombok.Data;
import org.apache.ibatis.type.EnumTypeHandler;

import java.math.BigDecimal;
import java.util.Date;

@TableName("deliveries")
@Data
public class Delivery {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("customer_name")
    private String customerName;

    @TableField("customer_phone")
    private String customerPhone;

    private String address;

    @TableField("pickup_latitude")
    private BigDecimal pickupLatitude;

    @TableField("pickup_longitude")
    private BigDecimal pickupLongitude;

    @TableField("destination_latitude")
    private BigDecimal destinationLatitude;

    @TableField("destination_longitude")
    private BigDecimal destinationLongitude;

    @TableField("driver_id")
    private Long driverId;

    /**
     * The AppUser who owns this delivery as a customer.
     * This is deliberately an ID from our database, not a value supplied by a JWT claim.
     */
    @TableField("owner_id")
    private Long ownerId;

    @TableField(value = "status", typeHandler = EnumTypeHandler.class)
    private DeliveryStatus status;

    @TableField(value = "priority", typeHandler = EnumTypeHandler.class)
    private DeliveryPriority priority;

    @TableField("package_weight_kg")
    private BigDecimal packageWeightKg;

    @TableField("time_window_start")
    private Date timeWindowStart;

    @TableField("time_window_end")
    private Date timeWindowEnd;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;

    @TableField("delivered_at")
    private Date deliveredAt;

    @Version
    @TableField("version")
    private Long version;
}
