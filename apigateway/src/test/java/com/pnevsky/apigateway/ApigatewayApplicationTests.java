package com.pnevsky.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ApigatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
