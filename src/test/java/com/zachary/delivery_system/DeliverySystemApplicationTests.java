package com.zachary.delivery_system;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliverySystemApplicationTests {

    @Test
    void applicationEnablesTheV3StaleDriverScheduler() {
        assertTrue(
                DeliverySystemApplication.class.isAnnotationPresent(
                        EnableScheduling.class
                )
        );
    }

}
