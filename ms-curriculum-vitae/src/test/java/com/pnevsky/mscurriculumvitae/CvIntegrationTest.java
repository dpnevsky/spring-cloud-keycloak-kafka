package com.pnevsky.mscurriculumvitae;

import com.pnevsky.mscurriculumvitae.usecasse.impl.CountryEventProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
@AutoConfigureMockMvc
@EmbeddedKafka(kraft = true, partitions = 3, topics = "country-name-topic", bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class CvIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CountryEventProjection projection;
    @Autowired KafkaTemplate<String, String> kafka;
    private static final AtomicInteger nameRequests = new AtomicInteger();
    private static volatile boolean unavailable;
    private static final HttpServer countryServer = startCountryServer();

    private static HttpServer startCountryServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/countries/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                int status = 200;
                String body;
                if (path.equals("/countries/country-name/1")) {
                    nameRequests.incrementAndGet();
                    status = unavailable ? 503 : 200;
                    body = unavailable ? "unavailable" : "Russia";
                } else if (path.equals("/countries/country-id/Russia")) {
                    body = "1";
                } else {
                    status = 404;
                    body = "not found";
                }
                exchange.getResponseHeaders().set("Content-Type", path.equals("/countries/country-id/Russia") ? "application/json" : "text/plain");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.start();
            return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @DynamicPropertySource
    static void countryUrl(DynamicPropertyRegistry properties) {
        properties.add("spring.cloud.openfeign.client.config.COUNTRY-SERVICE.url",
                () -> "http://127.0.0.1:" + countryServer.getAddress().getPort());
    }

    @AfterAll
    static void stopCountryServer() { countryServer.stop(0); }

    @BeforeEach
    void configureCountries() {
        unavailable = false;
        nameRequests.set(0);
    }

    @Test
    void readsExistingCvAndReturns404ForMissingCv() throws Exception {
        mvc.perform(get("/cv/1")).andExpect(status().isOk()).andExpect(jsonPath("$.countryName").value("Russia"));
        mvc.perform(get("/cv/9999")).andExpect(status().isNotFound());
    }

    @Test
    void createsCvWithServerUuidAndLocation() throws Exception {
        String requestedUuid = UUID.randomUUID().toString();
        var result = mvc.perform(post("/cv").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("uuid", requestedUuid, "name", "Alex", "surname", "Ivanov", "countryName", "Russia"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        String uuid = json.readTree(result.getResponse().getContentAsString()).get("uuid").asText();
        assertNotEquals(requestedUuid, uuid);
        assertNotNull(UUID.fromString(uuid));
        mvc.perform(get(result.getResponse().getHeader("Location"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex")).andExpect(jsonPath("$.countryName").value("Russia"));
    }

    @Test
    void invalidCvReturns400() throws Exception {
        mvc.perform(post("/cv").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingCountryReturns404() throws Exception {
        mvc.perform(post("/cv").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Alex", "surname", "Ivanov", "countryName", "Unknown"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unavailableCountryReturns503InsteadOfFakeCountry() throws Exception {
        unavailable = true;
        mvc.perform(get("/cv/1")).andExpect(status().isServiceUnavailable());
        assertEquals(2, nameRequests.get());
    }

    @Test
    void consumesKafkaEventAndExposesLastValue() throws Exception {
        mvc.perform(get("/cv/country-events/42")).andExpect(status().isNotFound());
        kafka.send("country-name-topic", "42", "Canada").get(20, TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(20)).until(() -> projection.find(42L).map(e -> e.countryName().equals("Canada")).orElse(false));
        kafka.send("country-name-topic", "42", "Updated Canada").get(20, TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(20)).until(() -> projection.find(42L).map(e -> e.countryName().equals("Updated Canada")).orElse(false));
        mvc.perform(get("/cv/country-events/42")).andExpect(status().isOk())
                .andExpect(jsonPath("$.countryId").value(42)).andExpect(jsonPath("$.countryName").value("Updated Canada"));
    }
}
