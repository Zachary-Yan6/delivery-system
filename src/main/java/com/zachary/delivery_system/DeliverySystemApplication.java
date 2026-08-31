package com.zachary.delivery_system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.zachary.delivery_system.mapper")
public class DeliverySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliverySystemApplication.class, args);
    }

}
