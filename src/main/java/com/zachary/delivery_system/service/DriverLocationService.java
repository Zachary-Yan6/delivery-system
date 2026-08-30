package com.zachary.delivery_system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.DriverLocation;

import java.util.List;

public interface DriverLocationService extends IService<DriverLocation> {

    DriverLocation recordLocation(
            AppUser currentUser,
            DriverLocationRequest request
    );

    List<DriverLatestLocationResponse> getLatestLocations();
}