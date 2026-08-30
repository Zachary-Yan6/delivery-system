package com.zachary.delivery_system.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zachary.delivery_system.entity.Delivery;

import java.util.List;


/**
* @author 22091
* @description 针对表【deliveries】的数据库操作Service
* @createDate 2026-08-23 11:14:34
*/
public interface DeliveryService extends IService<Delivery> {

    Delivery assignDriver(Long deliveryId, Long driverId);

    List<Delivery> getDeliveriesForDriver(Long driverId);

    Delivery startDelivery(Long driverId, Long deliveryId);

    Delivery markDelivered(Long driverId, Long deliveryId);

    Delivery markFailed(Long driverId, Long deliveryId);

}
