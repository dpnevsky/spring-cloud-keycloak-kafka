package com.pnevsky.cvsappconfigserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CvsAppConfigServerApplicationTests {
    @Autowired MockMvc mvc;

    @Test
    void servesNativeFeignConfiguration() throws Exception {
        mvc.perform(get("/CURRICULUM-VITAE-SERVICE/default")).andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source['spring.cloud.openfeign.client.config.COUNTRY-SERVICE.connectTimeout']").value(2000));
    }
}
