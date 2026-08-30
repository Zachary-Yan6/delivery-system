package com.zachary.delivery_system.dto.Auth;

public record LoginResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String role
) {
}