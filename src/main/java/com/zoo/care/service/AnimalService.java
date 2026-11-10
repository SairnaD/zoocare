package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
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
        return AnimalResponse.from(getAnimal(id));
    }

    @Transactional
    public AnimalResponse create(AnimalRequest request) {
        String name = request.name().trim();
        if (animalRepository.existsByNameIgnoreCase(name)) {
            throw new AnimalAlreadyExistsException(name);
        }
        Species species = getSpecies(request.speciesId());
        Enclosure enclosure = lockEnclosure(request.enclosureId());
        checkPlacement(species, enclosure);

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
        Animal animal = getPresentAnimal(id);
        String name = request.name().trim();
        if (animalRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new AnimalAlreadyExistsException(name);
        }
        Species species = getSpecies(request.speciesId());

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
        Animal animal = getPresentAnimal(animalId);
        if (animal.getEnclosure().getId().equals(targetEnclosureId)) {
            throw new SameEnclosureException();
        }
        moveToEnclosure(animal, animal.getSpecies(), targetEnclosureId);
        return AnimalResponse.from(animal);
    }

    @Transactional
    public void delete(Long id) {
        Animal animal = getAnimal(id);
        try {
            animalRepository.delete(animal);
            animalRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.info("Animal id={} cannot be deleted: other records refer to it", id);
            throw new AnimalInUseException();
        }
        log.info("Deleted animal '{}' (id={})", animal.getName(), id);
    }

    private void moveToEnclosure(Animal animal, Species species, Long targetId) {
        Enclosure target = lockEnclosure(targetId);
        checkPlacement(species, target);

        String sourceName = animal.getEnclosure().getName();
        animal.setEnclosure(target);
        log.info("Transferred animal '{}' from '{}' to '{}'", animal.getName(), sourceName, target.getName());
    }

    private void checkPlacement(Species species, Enclosure enclosure) {
        checkHabitat(species, enclosure);
        checkCapacity(enclosure);
    }

    private void checkHabitat(Species species, Enclosure enclosure) {
        if (species.getHabitatType() != enclosure.getHabitatType()) {
            throw new HabitatMismatchException(species.getName(), enclosure.getName());
        }
    }

    private void checkCapacity(Enclosure enclosure) {
        if (animalRepository.countByEnclosureId(enclosure.getId()) >= enclosure.getCapacity()) {
            throw new EnclosureFullException(enclosure.getName());
        }
    }

    private void flushOrReportDuplicate(String name) {
        try {
            animalRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AnimalAlreadyExistsException(name);
        }
    }

    private Animal getAnimal(Long id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", id));
    }

    private Animal getPresentAnimal(Long id) {
        Animal animal = getAnimal(id);
        if (animal.getDeletedStatus() != DeletedStatus.PRESENT) {
            throw new AnimalDeletedException();
        }
        return animal;
    }

    private Species getSpecies(Long id) {
        return speciesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Species", id));
    }

    private Enclosure lockEnclosure(Long id) {
        return enclosureRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", id));
    }
}