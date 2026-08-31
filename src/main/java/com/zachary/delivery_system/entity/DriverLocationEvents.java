package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName driver_location_events
 */
@TableName(value ="driver_location_events")
@Data
public class DriverLocationEvents {
    /**
     * 
     */
    @TableId
    private Date received_at;

    /**
     * 
     */
    @TableId
    private Object event_id;

    /**
     * 
     */
    private Long driver_id;

    /**
     * 
     */
    private BigDecimal latitude;

    /**
     * 
     */
    private BigDecimal longitude;

    /**
     * 
     */
    private Date recorded_at;
}