package com.pnevsky.mscurriculumvitae.usecasse.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final CountryEventProjection projection;

    @KafkaListener(topics = "${app.kafka.country-topic}")
    public void consumeMessage(String name, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        projection.update(Long.parseLong(key), name);
        log.info("Received country event for id {}", key);
    }
}
