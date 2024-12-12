package com.pnevsky.mscurriculumvitae.usecasse.impl;

import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CountryEventProjection {
    private final ConcurrentMap<Long, CountryEvent> events = new ConcurrentHashMap<>();

    public void update(Long id, String name) { events.put(id, new CountryEvent(id, name)); }
    public Optional<CountryEvent> find(Long id) { return Optional.ofNullable(events.get(id)); }

    public record CountryEvent(Long countryId, String countryName) {}
}
