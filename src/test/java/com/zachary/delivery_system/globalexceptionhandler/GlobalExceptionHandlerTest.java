package com.zachary.delivery_system.globalexceptionhandler;

import com.zachary.delivery_system.exception.ApiErrorResponse;
import com.zachary.delivery_system.exception.ApiErrorResponseFactory;
import com.zachary.delivery_system.exception.DeliveryAlreadyAssignedException;
import com.zachary.delivery_system.exception.DeliveryNotFoundException;
import com.zachary.delivery_system.security.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new ApiErrorResponseFactory());

    @Test
    void handleApiException_returnsAConsistentDomainErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/deliveries/42"
        );
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "abc123");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleApiException(
                        new DeliveryNotFoundException(42L),
                        request
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().error());
        assertEquals("DELIVERY_NOT_FOUND", response.getBody().code());
        assertEquals("Delivery not found: 42", response.getBody().message());
        assertEquals("/api/deliveries/42", response.getBody().path());
        assertEquals("abc123", response.getBody().traceId());
    }

    @Test
    void handleApiException_returnsTheAssignmentConflictContract() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/deliveries/123/assignment"
        );
        request.setAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE, "abc123");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleApiException(
                        new DeliveryAlreadyAssignedException(),
                        request
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("CONFLICT", response.getBody().error());
        assertEquals(
                "DELIVERY_ALREADY_ASSIGNED",
                response.getBody().code()
        );
        assertEquals(
                "Delivery has already been assigned",
                response.getBody().message()
        );
        assertEquals(
                "/api/deliveries/123/assignment",
                response.getBody().path()
        );
        assertEquals("abc123", response.getBody().traceId());
    }
}
