package com.zoo.care.service;

import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final AnimalRepository animalRepository;

    public List<SpeciesResponse> findAll() {
        return speciesRepository.findAll(Sort.by("name")).stream()
                .map(SpeciesResponse::from)
                .toList();
    }

    public SpeciesResponse findById(Long id) {
        return SpeciesResponse.from(getSpecies(id));
    }

    @Transactional
    public SpeciesResponse create(SpeciesRequest request) {
        String name = request.name().trim();
        if (speciesRepository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Species species = Species.builder()
                .name(name)
                .habitatType(request.habitatType())
                .dietType(request.dietType())
                .build();
        Species saved = speciesRepository.save(species);
        log.info("Created species '{}' (id={})", saved.getName(), saved.getId());
        return SpeciesResponse.from(saved);
    }

    @Transactional
    public SpeciesResponse update(Long id, SpeciesRequest request) {
        Species species = getSpecies(id);
        String name = request.name().trim();
        if (speciesRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        if (species.getHabitatType() != request.habitatType()
                && animalRepository.existsBySpeciesId(id)) {
            throw new BusinessRuleException("Habitat cannot be changed while the species has animals");
        }
        species.setName(name);
        species.setHabitatType(request.habitatType());
        species.setDietType(request.dietType());
        log.info("Updated species '{}' (id={})", name, id);
        return SpeciesResponse.from(species);
    }

    @Transactional
    public void delete(Long id) {
        Species species = getSpecies(id);
        try {
            speciesRepository.delete(species);
            speciesRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleException("Species is in use and cannot be deleted");
        }
        log.info("Deleted species '{}' (id={})", species.getName(), id);
    }

    private Species getSpecies(Long id) {
        return speciesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Species", id));
    }

    private BusinessRuleException duplicateName(String name) {
        return new BusinessRuleException("Species with name '" + name + "' already exists");
    }
}