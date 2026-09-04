package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zachary.delivery_system.dto.Driver.CreateDriverRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.UsernameAlreadyExistsException;
import com.zachary.delivery_system.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private AppUserService appUserService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LambdaQueryChainWrapper<AppUser> userQuery;

    private DriverServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new DriverServiceImpl(passwordEncoder));
        ReflectionTestUtils.setField(service, "appUserService", appUserService);
        when(appUserService.lambdaQuery()).thenReturn(userQuery);
        when(userQuery.eq(any(), any())).thenReturn(userQuery);
    }

    @Test
    void createDriverCreatesLoginAndAppliesDefaults() {
        CreateDriverRequest request = request();
        request.setAvailable(null);
        request.setVehicleCapacityKg(null);
        when(userQuery.exists()).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(appUserService.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(20L);
            return true;
        });
        doReturn(true).when(service).save(any(Driver.class));

        Driver result = service.createDriver(request);

        assertEquals(20L, result.getUserId());
        assertEquals("Driver One", result.getFullName());
        assertEquals("0700000000", result.getPhone());
        assertTrue(result.getActive());
        assertTrue(result.getAvailable());
        assertEquals(new BigDecimal("100.00"), result.getVehicleCapacityKg());
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserService).save(userCaptor.capture());
        assertEquals("driver1", userCaptor.getValue().getUsername());
        assertEquals("encoded", userCaptor.getValue().getPasswordHash());
        assertEquals("DRIVER", userCaptor.getValue().getRole());
    }

    @Test
    void createDriverUsesProvidedAvailabilityAndCapacity() {
        CreateDriverRequest request = request();
        request.setAvailable(false);
        request.setVehicleCapacityKg(new BigDecimal("250.00"));
        when(userQuery.exists()).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(appUserService.save(any(AppUser.class))).thenAnswer(invocation -> {
            ((AppUser) invocation.getArgument(0)).setId(21L);
            return true;
        });
        doReturn(true).when(service).save(any(Driver.class));

        Driver result = service.createDriver(request);

        assertFalse(result.getAvailable());
        assertEquals(new BigDecimal("250.00"), result.getVehicleCapacityKg());
    }

    @Test
    void createDriverRejectsADuplicateUsernameBeforeWriting() {
        when(userQuery.exists()).thenReturn(true);

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> service.createDriver(request())
        );

        verify(appUserService, never()).save(any());
        verify(service, never()).save(any(Driver.class));
    }

    private CreateDriverRequest request() {
        CreateDriverRequest request = new CreateDriverRequest();
        request.setUsername("driver1");
        request.setPassword("password123");
        request.setFullName("Driver One");
        request.setPhone("0700000000");
        return request;
    }
}
