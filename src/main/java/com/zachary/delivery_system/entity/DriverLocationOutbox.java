package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("driver_location_outbox")
public class DriverLocationOutbox {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("event_type")
    private String eventType;

    @TableField("aggregate_id")
    private Long aggregateId;

    private String payload;

    private String status;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("next_attempt_at")
    private Instant nextAttemptAt;

    @TableField("lock_token")
    private UUID lockToken;

    @TableField("locked_until")
    private Instant lockedUntil;

    @TableField("published_at")
    private Instant publishedAt;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private Instant createdAt;
}