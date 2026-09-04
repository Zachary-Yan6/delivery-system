package com.zachary.delivery_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("audit_events")
public class AuditEvent {

    @TableId(value = "id", type = IdType.INPUT)
    private UUID id;

    @TableField("actor_user_id")
    private Long actorUserId;

    @TableField("actor_username")
    private String actorUsername;

    @TableField("actor_role")
    private String actorRole;

    private String action;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private Long entityId;

    private String details;

    @TableField("trace_id")
    private String traceId;

    @TableField("occurred_at")
    private Instant occurredAt;
}