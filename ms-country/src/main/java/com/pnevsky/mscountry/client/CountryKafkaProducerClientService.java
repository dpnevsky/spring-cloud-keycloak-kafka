package com.pnevsky.mscountry.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class CountryKafkaProducerClientService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public CountryKafkaProducerClientService(KafkaTemplate<String, String> kafkaTemplate,
                                             @Value("${app.kafka.country-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public CompletableFuture<SendResult<String, String>> sendCountryName(Long countryId, String countryName) {
        try {
            return kafkaTemplate.send(topic, countryId.toString(), countryName);
        } catch (org.apache.kafka.common.KafkaException | org.springframework.kafka.KafkaException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
