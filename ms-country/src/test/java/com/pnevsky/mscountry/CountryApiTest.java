package com.pnevsky.mscountry;

import com.pnevsky.mscountry.client.CountryKafkaProducerClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.kafka.admin.auto-create=false"})
@AutoConfigureMockMvc
class CountryApiTest {
    @Autowired MockMvc mvc;
    @MockBean CountryKafkaProducerClientService producer;

    @Test
    void getReturnsCountryWithoutPublishingEvent() throws Exception {
        mvc.perform(get("/countries/country-name/1")).andExpect(status().isOk()).andExpect(content().string("Russia"));
        mvc.perform(get("/countries/country-id/Russia")).andExpect(status().isOk()).andExpect(content().string("1"));
        verifyNoInteractions(producer);
    }

    @Test
    void missingCountriesReturn404() throws Exception {
        mvc.perform(get("/countries/country-name/9999")).andExpect(status().isNotFound());
        mvc.perform(get("/countries/country-id/Unknown")).andExpect(status().isNotFound());
        mvc.perform(post("/countries/9999/events")).andExpect(status().isNotFound());
        verifyNoInteractions(producer);
    }

    @Test
    void acceptsEventOnlyAfterKafkaAcknowledgement() throws Exception {
        var acknowledgement = new CompletableFuture<SendResult<String, String>>();
        when(producer.sendCountryName(1L, "Russia")).thenReturn(acknowledgement);
        var result = mvc.perform(post("/countries/1/events")).andExpect(request().asyncStarted()).andReturn();
        acknowledgement.complete(null);
        mvc.perform(asyncDispatch(result)).andExpect(status().isAccepted());
        verify(producer).sendCountryName(1L, "Russia");
    }

    @Test
    void publicationFailureReturns503() throws Exception {
        when(producer.sendCountryName(1L, "Russia")).thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
        var result = mvc.perform(post("/countries/1/events")).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isServiceUnavailable());
    }
}
