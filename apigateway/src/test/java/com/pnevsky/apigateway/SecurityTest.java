package com.pnevsky.apigateway;

import com.pnevsky.apigateway.config.SecurityConfig;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {
    private static final String ISSUER = "https://keycloak.example/realms/cvs";
    private HttpServer server;
    private RSAKey key;
    private SecurityConfig config;
    private ReactiveJwtDecoder decoder;

    @BeforeEach
    void startJwksServer() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        byte[] jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (var out = exchange.getResponseBody()) { out.write(jwks); }
        });
        server.start();
        config = new SecurityConfig(ISSUER, "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks", "cvs-api");
        decoder = config.jwtDecoder();
    }

    @AfterEach
    void stopServer() { server.stop(0); }

    private String token(String issuer, String audience, long expiresInMillis, RSAKey signingKey, String... roles) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("service-account")
                .issuer(issuer).audience(audience).issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + expiresInMillis))
                .claim("realm_access", Map.of("roles", List.of(roles))).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    @Test
    void acceptsExpectedIssuerAudienceAndRoles() throws Exception {
        var jwt = decoder.decode(token(ISSUER, "cvs-api", 300000, key, "cv-read")).block(Duration.ofSeconds(10));
        var auth = config.jwtAuthenticationConverter().convert(jwt).block(Duration.ofSeconds(10));
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_cv-read")));
    }

    @Test
    void rejectsWrongIssuer() throws Exception {
        String jwt = token("https://other.example", "cvs-api", 300000, key);
        assertThrows(Exception.class, () -> decoder.decode(jwt).block(Duration.ofSeconds(10)));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String jwt = token(ISSUER, "another-api", 300000, key);
        assertThrows(Exception.class, () -> decoder.decode(jwt).block(Duration.ofSeconds(10)));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String jwt = token(ISSUER, "cvs-api", -120000, key);
        assertThrows(Exception.class, () -> decoder.decode(jwt).block(Duration.ofSeconds(10)));
    }

    @Test
    void rejectsWrongSignature() throws Exception {
        String jwt = token(ISSUER, "cvs-api", 300000, new RSAKeyGenerator(2048).generate());
        assertThrows(Exception.class, () -> decoder.decode(jwt).block(Duration.ofSeconds(10)));
    }

    @Test
    void enforcesReadAndWriteRoles() throws Exception {
        try (var context = new GenericApplicationContext()) {
            context.registerBean(ReactiveJwtDecoder.class, () -> decoder);
            context.refresh();
            var http = ServerHttpSecurity.http();
            ReflectionTestUtils.invokeMethod(http, "setApplicationContext", context);
            var client = WebTestClient.bindToController(new ProbeController())
                    .webFilter(new WebFilterChainProxy(config.securityWebFilterChain(http))).build();
            client.get().uri("/actuator/health").exchange().expectStatus().isOk();
            client.get().uri("/cv/1").exchange().expectStatus().isUnauthorized();
            String reader = token(ISSUER, "cvs-api", 300000, key, "cv-read");
            String writer = token(ISSUER, "cvs-api", 300000, key, "cv-read", "cv-write");
            client.get().uri("/cv/1").headers(h -> h.setBearerAuth(reader)).exchange().expectStatus().isOk();
            client.get().uri("/countries/country-name/1").headers(h -> h.setBearerAuth(reader)).exchange().expectStatus().isOk();
            client.post().uri("/cv").headers(h -> h.setBearerAuth(reader)).exchange().expectStatus().isForbidden();
            client.post().uri("/countries/1/events").headers(h -> h.setBearerAuth(reader)).exchange().expectStatus().isForbidden();
            client.post().uri("/cv").headers(h -> h.setBearerAuth(writer)).exchange().expectStatus().isCreated();
            client.post().uri("/countries/1/events").headers(h -> h.setBearerAuth(writer)).exchange().expectStatus().isAccepted();
            client.get().uri("/other").headers(h -> h.setBearerAuth(writer)).exchange().expectStatus().isForbidden();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/actuator/health") String health() { return "UP"; }
        @GetMapping({"/cv/1", "/countries/country-name/1"}) String get() { return "data"; }
        @PostMapping("/cv") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) void create() {}
        @PostMapping("/countries/1/events") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED) void event() {}
    }
}
