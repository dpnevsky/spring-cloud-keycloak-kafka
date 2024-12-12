package com.pnevsky.mscountry.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Bean
    NewTopic countryTopic(@Value("${app.kafka.country-topic}") String topic,
                         @Value("${app.kafka.replication-factor}") int replicas,
                         @Value("${app.kafka.min-in-sync-replicas}") int minInSyncReplicas) {
        return TopicBuilder.name(topic).partitions(3).replicas(replicas)
                .configs(Map.of("min.insync.replicas", Integer.toString(minInSyncReplicas))).build();
    }
}
