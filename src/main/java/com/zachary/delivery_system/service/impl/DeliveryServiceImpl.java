package com.zachary.delivery_system.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zachary.delivery_system.entity.Delivery;
import com.zachary.delivery_system.entity.Driver;
import com.zachary.delivery_system.mapper.DriverLocationMapper;
import com.zachary.delivery_system.service.DeliveryService;
import com.zachary.delivery_system.mapper.DeliveryMapper;
import com.zachary.delivery_system.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

/**
* @author 22091
* @description 针对表【deliveries】的数据库操作Service实现
* @createDate 2026-08-23 11:14:34
*/
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl extends ServiceImpl<DeliveryMapper, Delivery>
    implements DeliveryService {


    private final DriverService driverService;

    @Override
    public Delivery assignDriver(Long deliveryId, Long driverId) {
        Delivery delivery = this.getById(deliveryId);

        // delivery does not exist.
        if (delivery == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Delivery not found: " + deliveryId
            );
        }

        // check if the status is created.
        if (!"CREATED".equals(delivery.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a CREATED delivery can be assigned"
            );
        }

        Driver driver = driverService.getById(driverId);

        // check if the driver exist
        if (driver == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Driver not found: " + driverId
            );
        }

        // check if the driver is able to be assigned
        if (!Boolean.TRUE.equals(driver.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot assign a delivery to an inactive driver"
            );
        }

        delivery.setDriverId(driverId);
        delivery.setStatus("ASSIGNED");
        delivery.setUpdatedAt(new Date());

        updateById(delivery);
        return delivery;
    }

    @Override
    public List<Delivery> getDeliveriesForDriver(Long driverId) {
        validateDriverExists(driverId);

        QueryWrapper<Delivery> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("driver_id", driverId);

        return list(queryWrapper);
    }

    @Override
    public Delivery startDelivery(Long driverId, Long deliveryId) {
        Delivery delivery = getDriverDelivery(driverId, deliveryId);

        changeStatus(delivery, "ASSIGNED", "IN_TRANSIT");
        return delivery;

    }


    @Override
    @Transactional
    public Delivery markDelivered(Long driverId, Long deliveryId) {
        Delivery delivery = getDriverDelivery(driverId, deliveryId);

        changeStatus(delivery, "IN_TRANSIT", "DELIVERED");
        delivery.setDeliveredAt(new Date());
        updateById(delivery);

        return delivery;
    }

    @Override
    @Transactional
    public Delivery markFailed(Long driverId, Long deliveryId) {
        Delivery delivery = getDriverDelivery(driverId, deliveryId);

        changeStatus(delivery, "IN_TRANSIT", "FAILED");
        return delivery;
    }


    private void validateDriverExists(Long driverId) {
        if (driverService.getById(driverId) == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Driver not found: " + driverId
            );
        }
    }

    private Delivery getDriverDelivery(Long driverId, Long deliveryId) {
        validateDriverExists(driverId);

        Delivery delivery = getById(deliveryId);

        if (delivery == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Delivery not found: " + deliveryId
            );
        }

        if (!driverId.equals(delivery.getDriverId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This delivery is not assigned to this driver"
            );
        }

        return delivery;
    }

    private void changeStatus(
            Delivery delivery,
            String expectedStatus,
            String newStatus
    ) {
        if (!expectedStatus.equals(delivery.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Delivery status must be " + expectedStatus
            );
        }

        delivery.setStatus(newStatus);
        delivery.setUpdatedAt(new Date());
        updateById(delivery);
    }
}




