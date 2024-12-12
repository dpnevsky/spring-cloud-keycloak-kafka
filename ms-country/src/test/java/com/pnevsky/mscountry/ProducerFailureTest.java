package com.pnevsky.mscountry;

import com.pnevsky.mscountry.client.CountryKafkaProducerClientService;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProducerFailureTest {
    @Test
    @SuppressWarnings("unchecked")
    void synchronousProducerFailureBecomesFailedFuture() {
        var template = (KafkaTemplate<String, String>) mock(KafkaTemplate.class);
        when(template.send("countries", "1", "Russia")).thenThrow(new KafkaException("no metadata"));
        var producer = new CountryKafkaProducerClientService(template, "countries");
        var failure = assertThrows(CompletionException.class, () -> producer.sendCountryName(1L, "Russia").join());
        assertInstanceOf(KafkaException.class, failure.getCause());
    }
}
