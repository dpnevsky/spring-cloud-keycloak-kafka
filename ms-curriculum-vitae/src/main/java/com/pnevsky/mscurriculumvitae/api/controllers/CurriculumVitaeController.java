package com.pnevsky.mscurriculumvitae.api.controllers;

import com.pnevsky.mscurriculumvitae.client.CountryLookupService;
import com.pnevsky.mscurriculumvitae.usecasse.CurriculumVitaeService;
import com.pnevsky.mscurriculumvitae.usecasse.dto.CurriculumVitaeDto;
import com.pnevsky.mscurriculumvitae.usecasse.mapper.CurriculumVitaeMapper;
import com.pnevsky.mscurriculumvitae.usecasse.impl.CountryEventProjection;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cv")
public class CurriculumVitaeController {
    private final CurriculumVitaeService service;
    private final CurriculumVitaeMapper mapper;
    private final CountryLookupService countries;
    private final CountryEventProjection events;

    @GetMapping("/{id}")
    public ResponseEntity<CurriculumVitaeDto> getCvOfUser(@PathVariable Long id) {
        var cv = service.getCvById(id);
        var response = mapper.fromEntityToDto(cv);
        response.setCountryName(countries.fetchCountryName(cv.getCountryId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CurriculumVitaeDto> save(@Valid @RequestBody CurriculumVitaeDto request) {
        var cv = mapper.fromDtoToEntity(request);
        cv.setCountryId(countries.fetchCountryId(request.getCountryName()));
        var saved = service.save(cv);
        var response = mapper.fromEntityToDto(saved);
        response.setCountryName(request.getCountryName());
        return ResponseEntity.created(URI.create("/cv/" + saved.getId())).body(response);
    }

    @GetMapping("/country-events/{countryId}")
    public ResponseEntity<CountryEventProjection.CountryEvent> getCountryEvent(@PathVariable Long countryId) {
        return ResponseEntity.of(events.find(countryId));
    }
}
