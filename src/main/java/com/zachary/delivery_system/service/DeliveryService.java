package com.zachary.delivery_system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zachary.delivery_system.entity.AppUser;
import com.zachary.delivery_system.entity.Delivery;

import java.util.List;


/**
* @author 22091
* @description 针对表【deliveries】的数据库操作Service
* @createDate 2026-08-23 11:14:34
*/
public interface DeliveryService extends IService<Delivery> {

    Delivery assignDriver(
            AppUser actor,
            Long deliveryId,
            Long driverId
    );

    Delivery autoAssignDriver(AppUser actor, Long deliveryId);

    List<Delivery> getDeliveriesForDriver(Long driverId);

    Delivery acceptDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery pickupDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery startDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery markDelivered(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery markFailed(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery retryDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery resumeDelivery(
            AppUser actor,
            Long driverId,
            Long deliveryId
    );

    Delivery confirmDelivery(AppUser actor, Long deliveryId);

    Delivery cancelDelivery(AppUser actor, Long deliveryId);

    Delivery markReturned(AppUser actor, Long deliveryId);

}
