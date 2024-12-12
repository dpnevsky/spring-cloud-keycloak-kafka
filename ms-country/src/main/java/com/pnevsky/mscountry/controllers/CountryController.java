package com.pnevsky.mscountry.controllers;

import com.pnevsky.mscountry.client.CountryKafkaProducerClientService;
import com.pnevsky.mscountry.model.Country;
import com.pnevsky.mscountry.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/countries")
public class CountryController {
    private final CountryRepository countryRepository;
    private final CountryKafkaProducerClientService producer;

    @GetMapping("/country-id/{countryName}")
    public Long getCountryId(@PathVariable String countryName) {
        return countryRepository.findByCountryName(countryName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found")).getId();
    }

    @GetMapping("/country-name/{countryId}")
    public String getCountryName(@PathVariable Long countryId) {
        return findCountry(countryId).getCountryName();
    }

    @PostMapping("/{countryId}/events")
    public CompletableFuture<ResponseEntity<Void>> publishCountryName(@PathVariable Long countryId) {
        Country country = findCountry(countryId);
        return producer.sendCountryName(countryId, country.getCountryName()).handle((result, error) -> {
            if (error != null) {
                log.warn("Could not publish country event for id {}", countryId, error);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            return ResponseEntity.accepted().build();
        });
    }

    private Country findCountry(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found"));
    }
}
