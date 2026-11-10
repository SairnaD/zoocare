package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.*;
import com.zoo.care.entity.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final EnclosureRepository enclosureRepository;

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
            throw new AnimalAlreadyExistsException(name);
        }
        Species species = speciesRepository.findById(request.speciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species", request.speciesId()));

        // Lock the enclosure first so two concurrent creations can't both pass the capacity check
        Enclosure enclosure = enclosureRepository.findByIdForUpdate(request.enclosureId())
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", request.enclosureId()));
        checkCapacity(enclosure);
        checkHabitat(species, enclosure);

        Animal saved = animalRepository.save(Animal.builder()
                .name(name)
                .species(species)
                .enclosure(enclosure)
                .birthDate(request.birthDate())
                .weight(request.weight())
                .feedingIntervalHours(request.feedingIntervalHours())
                .description(request.description())
                .deletedStatus(DeletedStatus.PRESENT)
                .build());
        flushOrReportDuplicate(name);

        log.info("Registered animal '{}' (id={}) in enclosure '{}'",
                saved.getName(), saved.getId(), enclosure.getName());
        return AnimalResponse.from(saved);
    }

    @Transactional
    public AnimalResponse update(Long id, AnimalRequest request) {
        Animal animal = findPresentAnimal(id);

        String name = request.name().trim();
        if (animalRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AnimalAlreadyExistsException(name);
        }

        Species species = speciesRepository.findById(request.speciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species", request.speciesId()));

        if (!animal.getEnclosure().getId().equals(request.enclosureId())) {
            moveToEnclosure(animal, species, request.enclosureId());
        } else if (!animal.getSpecies().getId().equals(species.getId())) {
            checkHabitat(species, animal.getEnclosure());
        }

        animal.setName(name);
        animal.setSpecies(species);
        animal.setBirthDate(request.birthDate());
        animal.setWeight(request.weight());
        animal.setFeedingIntervalHours(request.feedingIntervalHours());
        animal.setDescription(request.description());
        flushOrReportDuplicate(name);

        log.info("Updated animal '{}' (id={})", name, id);
        return AnimalResponse.from(animal);
    }

    @Transactional
    public AnimalResponse transferAnimal(Long animalId, Long targetEnclosureId) {
        Animal animal = findPresentAnimal(animalId);
        if (animal.getEnclosure().getId().equals(targetEnclosureId)) {
            throw new SameEnclosureException();
        }
        moveToEnclosure(animal, animal.getSpecies(), targetEnclosureId);
        return AnimalResponse.from(animal);
    }

    @Transactional
    public void delete(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", id));
        animalRepository.delete(animal);
        animalRepository.flush();
        log.info("Deleted animal '{}' (id={})", animal.getName(), id);
    }

    private void moveToEnclosure(Animal animal, Species species, Long targetId) {
        Enclosure target = enclosureRepository.findByIdForUpdate(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", targetId));
        checkHabitat(species, target);
        checkCapacity(target);

        Enclosure source = animal.getEnclosure();
        animal.setEnclosure(target);
        log.info("Transferred animal '{}' from '{}' to '{}'",
                animal.getName(), source.getName(), target.getName());
    }

    private void checkCapacity(Enclosure target) {
        if (animalRepository.countByEnclosureId(target.getId()) >= target.getCapacity()) {
            throw new EnclosureFullException(target.getName());
        }
    }

    private void checkHabitat(Species species, Enclosure enclosure) {
        if (species.getHabitatType() != enclosure.getHabitatType()) {
            throw new HabitatMismatchException(species.getName(), enclosure.getName());
        }
    }

    private Animal findPresentAnimal(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", id));
        if (animal.getDeletedStatus() != DeletedStatus.PRESENT) {
            throw new AnimalDeletedException();
        }
        return animal;
    }
    private void flushOrReportDuplicate(String name) {
        try {
            animalRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AnimalAlreadyExistsException(name);
        }
    }
}