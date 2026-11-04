package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.*;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final EnclosureRepository enclosureRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAllAnimals() {
        return animalRepository.findAll().stream().map(AnimalResponse::from).toList();
    }

    @Transactional
    public Animal createAnimal(AnimalRequest request) {
        if (animalRepository.existsByName(request.name())) {
            throw new BusinessRuleException("Animal with name already exists!");
        }
        Species species = speciesRepository.findById(request.speciesId())
                .orElseThrow(() -> new BusinessRuleException("Species not found"));
        Enclosure enclosure = enclosureRepository.findById(request.enclosureId())
                .orElseThrow(() -> new BusinessRuleException("Enclosure not found"));
        if(enclosure.getCapacity() == animalRepository.findByEnclosureId(enclosure.getId()).size()){
            throw new BusinessRuleException("Enclosure already full!");
        }
        if(species.getHabitatType() != enclosure.getHabitatType()){
            throw new BusinessRuleException("Habitat types don't match!");
        }
        String photoFile = fileStorageService.store(request.photo());

        Animal animal = Animal.builder()
                .name(request.name())
                .species(species)
                .enclosure(enclosure)
                .birthdate(request.birthDate())
                .weight(request.weight())
                .size(request.size())
                .feedingInterval(Duration.ofHours(request.feedingIntervalHours()))
                .description(request.description())
                .photo(photoFile)
                .healthStatus(HealthStatus.HEALTHY)
                .deletedStatus(DeletedStatus.PRESENT)
                .build();

        Animal saved = animalRepository.save(animal);
        log.info("Registered new animal '{}'", saved.getName());
        return saved;
    }
}