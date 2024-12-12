package com.pnevsky.mscountry;

import com.pnevsky.mscountry.client.CountryKafkaProducerClientService;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"eureka.client.enabled=false"})
@EmbeddedKafka(count = 3, kraft = true, partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class CountryKafkaTest {
    @Autowired CountryKafkaProducerClientService producer;
    @Autowired EmbeddedKafkaBroker broker;

    @Test
    void publishesCountryIdAndNameToReplicatedTopic() throws Exception {
        var properties = KafkaTestUtils.consumerProps("country-test", "false", broker);
        properties.put("auto.offset.reset", "earliest");
        try (var consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(java.util.List.of("country-name-topic"));
            var result = producer.sendCountryName(7L, "Germany").get(20, TimeUnit.SECONDS);
            assertEquals("7", result.getProducerRecord().key());
            var record = KafkaTestUtils.getSingleRecord(consumer, "country-name-topic", Duration.ofSeconds(20));
            assertEquals("7", record.key());
            assertEquals("Germany", record.value());
            assertEquals(3, consumer.partitionsFor("country-name-topic").size());
            assertEquals(3, consumer.partitionsFor("country-name-topic").get(0).replicas().length);
        }
    }
}
