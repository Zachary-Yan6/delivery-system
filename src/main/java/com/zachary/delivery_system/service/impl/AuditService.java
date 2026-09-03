package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.AuditEvent;
import com.zachary.delivery_system.enums.AuditAction;
import com.zachary.delivery_system.mapper.AuditEventMapper;
import lombok.RequiredArgsConstructor;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventMapper auditEventMapper;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            AppUser actor,
            AuditAction action,
            String entityType,
            Long entityId,
            Map<String, Object> details
    ) {
        AuditEvent auditEvent = new AuditEvent();

        auditEvent.setId(UUID.randomUUID());
        auditEvent.setActorUserId(actor.getId());
        auditEvent.setActorUsername(actor.getUsername());
        auditEvent.setActorRole(actor.getRole());
        auditEvent.setAction(action.name());
        auditEvent.setEntityType(entityType);
        auditEvent.setEntityId(entityId);
        auditEvent.setDetails(toJson(details));
        auditEvent.setTraceId(MDC.get("traceId"));
        auditEvent.setOccurredAt(Instant.now());

        auditEventMapper.insertAuditEvent(auditEvent);
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize audit details",
                    exception
            );
        }
    }
}