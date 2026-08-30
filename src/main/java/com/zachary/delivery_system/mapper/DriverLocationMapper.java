package com.zachary.delivery_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.entity.DriverLocation;

import java.util.List;

public interface DriverLocationMapper extends BaseMapper<DriverLocation> {

    List<DriverLatestLocationResponse> selectLatestLocations();
}