package com.pnevsky.mscurriculumvitae.usecasse.impl;

import com.pnevsky.mscurriculumvitae.model.CurriculumVitae;
import com.pnevsky.mscurriculumvitae.persistence.repository.CurriculumVitaeRepository;
import com.pnevsky.mscurriculumvitae.usecasse.CurriculumVitaeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurriculumVitaeServiceImpl implements CurriculumVitaeService {
    private final CurriculumVitaeRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CurriculumVitae getCvById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found"));
    }

    @Override
    @Transactional
    public CurriculumVitae save(CurriculumVitae cv) {
        cv.setUuid(UUID.randomUUID());
        return repository.save(cv);
    }
}
