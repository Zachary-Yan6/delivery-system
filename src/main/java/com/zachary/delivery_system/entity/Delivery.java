package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    @TableField("destination_latitude")
    private BigDecimal destinationLatitude;

    @TableField("destination_longitude")
    private BigDecimal destinationLongitude;

    @TableField("driver_id")
    private Long driverId;

    private String status;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;

    @TableField("delivered_at")
    private Date deliveredAt;
}