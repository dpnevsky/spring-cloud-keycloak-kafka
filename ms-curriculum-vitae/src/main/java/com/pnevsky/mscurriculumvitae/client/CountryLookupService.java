package com.pnevsky.mscurriculumvitae.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CountryLookupService {
    private final CountryServiceFeignClient client;

    @Retry(name = "country-service")
    @CircuitBreaker(name = "country-service")
    public String fetchCountryName(Long id) { return client.fetchCountryName(id); }

    @Retry(name = "country-service")
    @CircuitBreaker(name = "country-service")
    public Long fetchCountryId(String name) { return client.fetchCountryId(name); }
}
