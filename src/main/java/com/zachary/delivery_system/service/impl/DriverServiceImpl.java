package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zachary.delivery_system.dto.Driver.CreateDriverRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.exception.UsernameAlreadyExistsException;
import com.zachary.delivery_system.service.AppUserService;
import com.zachary.delivery_system.service.DriverService;
import com.zachary.delivery_system.mapper.DriverMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
* @author 22091
* @description 针对表【drivers】的数据库操作Service实现
* @createDate 2026-08-23 11:14:55
*/
@Service
@RequiredArgsConstructor
public class DriverServiceImpl extends ServiceImpl<DriverMapper, Driver>
    implements DriverService {

    @Resource
    private AppUserService appUserService;

    @Resource
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public Driver createDriver(CreateDriverRequest request) {
        boolean usernameExists = appUserService.lambdaQuery()
                .eq(AppUser::getUsername, request.getUsername())
                .exists();

        if (usernameExists) {
            throw new UsernameAlreadyExistsException();
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("DRIVER");
        appUserService.save(user);

        Driver driver = new Driver();
        driver.setUserId(user.getId());
        driver.setFullName(request.getFullName());
        driver.setPhone(request.getPhone());
        driver.setActive(true);
        driver.setAvailable(
                !Boolean.FALSE.equals(request.getAvailable())
        );
        driver.setVehicleCapacityKg(
                request.getVehicleCapacityKg() == null
                        ? new BigDecimal("100.00")
                        : request.getVehicleCapacityKg()
        );
        save(driver);

        return driver;
    }
}




