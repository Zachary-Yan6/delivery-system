package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@TableName("drivers")
@Data
public class Driver {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("full_name")
    private String fullName;

    private String phone;

    private Boolean active;

    private Boolean available;

    @TableField("vehicle_capacity_kg")
    private BigDecimal vehicleCapacityKg;

    @Version
    private Long version;

    @TableField("created_at")
    private Date createdAt;
}
