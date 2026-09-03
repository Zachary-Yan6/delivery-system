package com.zachary.delivery_system.security;

import com.zachary.delivery_system.exception.ApiErrorResponse;
import com.zachary.delivery_system.exception.ApiErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestSecurityErrorHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApiErrorResponseFactory errorResponseFactory;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        write(
                response,
                errorResponseFactory.create(
                        HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "Authentication required",
                        request
                )
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        write(
                response,
                errorResponseFactory.create(
                        HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "You do not have permission to access this resource",
                        request
                )
        );
    }

    private void write(
            HttpServletResponse response,
            ApiErrorResponse errorResponse
    ) throws IOException {
        response.setStatus(errorResponse.status());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(TraceIdFilter.TRACE_ID_HEADER, errorResponse.traceId());
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
