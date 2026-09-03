package com.zachary.delivery_system.exception;

import com.zachary.delivery_system.security.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiErrorResponseFactory {

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                code,
                message,
                request.getRequestURI(),
                TraceIdFilter.getOrCreateTraceId(request)
        );
    }
}
