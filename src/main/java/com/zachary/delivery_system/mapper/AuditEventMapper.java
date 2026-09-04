package com.zachary.delivery_system.mapper;

import com.zachary.delivery_system.entity.AuditEvent;
import org.apache.ibatis.annotations.Insert;

public interface AuditEventMapper {

    @Insert("""
            INSERT INTO audit_events (
                id,
                actor_user_id,
                actor_username,
                actor_role,
                action,
                entity_type,
                entity_id,
                details,
                trace_id,
                occurred_at
            )
            VALUES (
                #{id},
                #{actorUserId},
                #{actorUsername},
                #{actorRole},
                #{action},
                #{entityType},
                #{entityId},
                CAST(#{details} AS JSONB),
                #{traceId},
                #{occurredAt}
            )
            """)
    int insertAuditEvent(AuditEvent auditEvent);
}