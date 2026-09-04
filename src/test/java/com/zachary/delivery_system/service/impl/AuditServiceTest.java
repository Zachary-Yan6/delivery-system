package com.zachary.delivery_system.service.impl;

import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.AuditEvent;
import com.zachary.delivery_system.enums.AuditAction;
import com.zachary.delivery_system.mapper.AuditEventMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventMapper auditEventMapper;
    @Mock
    private ObjectMapper objectMapper;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void recordPersistsActorActionDetailsAndTraceId() throws Exception {
        AppUser actor = new AppUser();
        actor.setId(2L);
        actor.setUsername("dispatcher");
        actor.setRole("DISPATCHER");
        Map<String, Object> details = Map.of("driverId", 8L);
        when(objectMapper.writeValueAsString(details))
                .thenReturn("{\"driverId\":8}");
        MDC.put("traceId", "trace-123");

        new AuditService(auditEventMapper, objectMapper).record(
                actor,
                AuditAction.DELIVERY_DRIVER_ASSIGNED,
                "DELIVERY",
                5L,
                details
        );

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventMapper).insertAuditEvent(captor.capture());
        AuditEvent event = captor.getValue();
        assertNotNull(event.getId());
        assertEquals(2L, event.getActorUserId());
        assertEquals("dispatcher", event.getActorUsername());
        assertEquals("DISPATCHER", event.getActorRole());
        assertEquals("DELIVERY_DRIVER_ASSIGNED", event.getAction());
        assertEquals("DELIVERY", event.getEntityType());
        assertEquals(5L, event.getEntityId());
        assertEquals("{\"driverId\":8}", event.getDetails());
        assertEquals("trace-123", event.getTraceId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void recordWrapsJsonSerializationFailure() throws Exception {
        Map<String, Object> details = Map.of("value", new Object());
        JacksonException jacksonException = mock(JacksonException.class);
        when(objectMapper.writeValueAsString(details))
                .thenThrow(jacksonException);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new AuditService(auditEventMapper, objectMapper).record(
                        new AppUser(),
                        AuditAction.DELIVERY_CANCELLED,
                        "DELIVERY",
                        5L,
                        details
                )
        );

        assertTrue(exception.getMessage().contains("serialize audit details"));
        assertSameCause(jacksonException, exception);
        verifyNoInteractions(auditEventMapper);
    }

    private void assertSameCause(
            Throwable expected,
            IllegalStateException actual
    ) {
        assertEquals(expected, actual.getCause());
    }
}
