package com.zachary.delivery_system.controller;

import com.zachary.delivery_system.dto.Auth.LoginRequest;
import com.zachary.delivery_system.dto.Auth.LoginResponse;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.exception.InvalidCredentialsException;
import com.zachary.delivery_system.security.JwtService;
import com.zachary.delivery_system.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login APIs")
public class AuthController {

    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Operation(summary = "Log in and receive a JWT access token")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = appUserService.lambdaQuery()
                .eq(AppUser::getUsername, request.getUsername())
                .one();

        if (user == null || !passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                user.getId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
