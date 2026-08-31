package com.zachary.delivery_system.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic driverLocationReportedTopic(
            @Value("${app.kafka.topics.driver-location-reported}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .config(
                        TopicConfig.RETENTION_MS_CONFIG,
                        Long.toString(Duration.ofDays(3).toMillis())
                )
                .build();
    }
}