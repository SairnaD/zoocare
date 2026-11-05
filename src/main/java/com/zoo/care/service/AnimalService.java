package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HealthStatus;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final EnclosureRepository enclosureRepository;
    private final FileStorageService fileStorageService;

    public List<AnimalResponse> findAll() {
        return animalRepository.findAll(Sort.by("name")).stream()
                .map(AnimalResponse::from)
                .toList();
    }

    public AnimalResponse findById(Long id) {
        return AnimalResponse.from(animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", id)));
    }

    @Transactional
    public AnimalResponse create(AnimalRequest request) {
        String name = request.name().trim();
        if (animalRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Animal with name '" + name + "' already exists");
        }
        Species species = speciesRepository.findById(request.speciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species", request.speciesId()));
        Enclosure enclosure = enclosureRepository.findById(request.enclosureId())
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", request.enclosureId()));
        checkPlacement(species, enclosure);

        String photoFile = fileStorageService.store(request.photo());
        Animal saved = animalRepository.save(Animal.builder()
                .name(name)
                .species(species)
                .enclosure(enclosure)
                .birthDate(request.birthDate())
                .weight(request.weight())
                .size(request.size())
                .feedingIntervalHours(request.feedingIntervalHours())
                .description(request.description())
                .photo(photoFile)
                .healthStatus(HealthStatus.HEALTHY)
                .build());
        log.info("Registered animal '{}' (id={}) in enclosure '{}'",
                saved.getName(), saved.getId(), enclosure.getName());
        return AnimalResponse.from(saved);
    }

    private void checkPlacement(Species species, Enclosure enclosure) {
        if (species.getHabitatType() != enclosure.getHabitatType()) {
            throw new BusinessRuleException("Habitat of species '" + species.getName()
                    + "' does not match enclosure '" + enclosure.getName() + "'");
        }
        if (animalRepository.countByEnclosureId(enclosure.getId()) >= enclosure.getCapacity()) {
            throw new BusinessRuleException("Enclosure '" + enclosure.getName() + "' is full");
        }
    }
}