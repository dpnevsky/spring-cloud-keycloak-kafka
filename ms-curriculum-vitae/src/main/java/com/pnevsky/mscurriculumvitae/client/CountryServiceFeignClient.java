package com.pnevsky.mscurriculumvitae.client;

import com.pnevsky.mscurriculumvitae.config.FeignErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "COUNTRY-SERVICE", configuration = FeignErrorDecoder.class)
public interface CountryServiceFeignClient {
    @GetMapping("/countries/country-name/{countryId}")
    String fetchCountryName(@PathVariable Long countryId);

    @GetMapping("/countries/country-id/{countryName}")
    Long fetchCountryId(@PathVariable String countryName);
}
