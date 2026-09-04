package com.zachary.delivery_system.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zachary.delivery_system.dto.Auth.LoginRequest;
import com.zachary.delivery_system.dto.Auth.LoginResponse;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.exception.InvalidCredentialsException;
import com.zachary.delivery_system.security.JwtService;
import com.zachary.delivery_system.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AppUserService appUserService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private LambdaQueryChainWrapper<AppUser> query;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                appUserService,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void loginReturnsTokenAndUserDetailsForValidCredentials() {
        LoginRequest request = request("dispatcher", "password123");
        AppUser user = user(2L, "dispatcher", "encoded", "DISPATCHER");
        stubUserQuery(user);
        when(passwordEncoder.matches("password123", "encoded"))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("signed-token");

        LoginResponse response = controller.login(request);

        assertEquals("signed-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(2L, response.userId());
        assertEquals("dispatcher", response.username());
        assertEquals("DISPATCHER", response.role());
        verify(jwtService).generateToken(user);
    }

    @Test
    void loginRejectsAnUnknownUsername() {
        LoginRequest request = request("missing", "password123");
        stubUserQuery(null);

        assertThrows(
                InvalidCredentialsException.class,
                () -> controller.login(request)
        );

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void loginRejectsAnIncorrectPassword() {
        LoginRequest request = request("driver", "wrong-password");
        AppUser user = user(3L, "driver", "encoded", "DRIVER");
        stubUserQuery(user);
        when(passwordEncoder.matches("wrong-password", "encoded"))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> controller.login(request)
        );

        verifyNoInteractions(jwtService);
    }

    private void stubUserQuery(AppUser result) {
        when(appUserService.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(result);
    }

    private LoginRequest request(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private AppUser user(
            Long id,
            String username,
            String passwordHash,
            String role
    ) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }
}
