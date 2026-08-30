package com.zachary.delivery_system.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zachary.delivery_system.dto.Location.DriverLatestLocationResponse;
import com.zachary.delivery_system.dto.Location.DriverLocationRequest;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.entity.DriverLocation;
import com.zachary.delivery_system.mapper.DriverLocationMapper;
import com.zachary.delivery_system.service.DriverLocationService;
import com.zachary.delivery_system.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl
        extends ServiceImpl<DriverLocationMapper, DriverLocation>
        implements DriverLocationService {

    private static final long MINIMUM_UPDATE_INTERVAL_MS = 5_000;
    private static final long MAX_FUTURE_TIME_MS = 5 * 60 * 1_000;

    private final DriverService driverService;
    private final DriverLocationMapper driverLocationMapper;

    @Override
    @Transactional
    public DriverLocation recordLocation(
            AppUser currentUser,
            DriverLocationRequest request
    ) {
        Driver driver = driverService.lambdaQuery()
                .eq(Driver::getUserId, currentUser.getId())
                .one();

        if (driver == null || !Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "An active driver account is required"
            );
        }

        Date now = new Date();

        if (request.getRecordedAt().after(
                new Date(now.getTime() + MAX_FUTURE_TIME_MS)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Recorded time cannot be more than five minutes in the future"
            );
        }

        DriverLocation latestLocation = this.lambdaQuery()
                .eq(DriverLocation::getDriverId, driver.getId())
                .orderByDesc(DriverLocation::getReceivedAt)
                .last("LIMIT 1")
                .one();

        if (latestLocation != null
                && now.getTime() - latestLocation.getReceivedAt().getTime()
                < MINIMUM_UPDATE_INTERVAL_MS) {

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Location updates may be sent only once every five seconds"
            );
        }

        DriverLocation location = new DriverLocation();
        location.setDriverId(driver.getId());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRecordedAt(request.getRecordedAt());
        location.setReceivedAt(now);

        this.save(location);
        return location;
    }

    @Override
    public List<DriverLatestLocationResponse> getLatestLocations() {
        return driverLocationMapper.selectLatestLocations();
    }
}