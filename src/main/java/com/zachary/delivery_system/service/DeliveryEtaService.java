package com.zachary.delivery_system.service;

import com.zachary.delivery_system.dto.Delivery.DeliveryEtaResponse;

public interface DeliveryEtaService {

    DeliveryEtaResponse calculateEta(Long deliveryId);
}