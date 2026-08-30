package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@TableName("driver_locations")
@Data
public class DriverLocation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("driver_id")
    private Long driverId;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @TableField("recorded_at")
    private Date recordedAt;

    @TableField("received_at")
    private Date receivedAt;
}