package com.pnevsky.msidentity;

import com.pnevsky.msidentity.repository.UserCredentialRepository;
import com.pnevsky.msidentity.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false", "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserCredentialRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired TestRestTemplate http;

    @Test
    void actualHttpErrorDispatchPreserves401And400() {
        assertEquals(401, http.getForEntity("/auth/validate", String.class).getStatusCode().value());
        var body = Map.of("username", "http-unicode", "email", "http-unicode@example.com", "password", "я".repeat(40));
        assertEquals(400, http.postForEntity("/auth/register", body, String.class).getStatusCode().value());
    }

    private String register(String username, String password) throws Exception {
        String body = json.writeValueAsString(Map.of("username", username, "email", username + "@example.com", "password", password));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        return body;
    }

    @Test
    void registersHashesPasswordAndIssuesValidToken() throws Exception {
        String username = "test-" + UUID.randomUUID();
        String password = "learning-password";
        String body = register(username, password);
        var stored = users.findByUsername(username).orElseThrow();
        assertNotEquals(password, stored.getPassword());
        assertTrue(encoder.matches(password, stored.getPassword()));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        var result = mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer")).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertDoesNotThrow(() -> jwt.validateToken(token));
        mvc.perform(get("/auth/validate").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRegistrationAndOverlongBcryptInput() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        String body = json.writeValueAsString(Map.of("username", "unicode", "email", "unicode@example.com", "password", "я".repeat(40)));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertTrue(users.findByUsername("unicode").isEmpty());
    }

    @Test
    void rejectsMissingMalformedAndTamperedToken() throws Exception {
        mvc.perform(get("/auth/validate")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/validate").header("Authorization", "Bearer ")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/validate").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        String token = jwt.generateToken("test");
        int signature = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signature) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signature) + replacement + token.substring(signature + 1);
        mvc.perform(get("/auth/validate").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
    }
}
