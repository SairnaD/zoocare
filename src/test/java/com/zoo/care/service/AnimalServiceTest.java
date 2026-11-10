package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.AnimalAlreadyExistsException;
import com.zoo.care.exception.EnclosureFullException;
import com.zoo.care.exception.HabitatMismatchException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    AnimalRepository animalRepository;
    @Mock
    SpeciesRepository speciesRepository;
    @Mock
    EnclosureRepository enclosureRepository;

    @InjectMocks
    AnimalService animalService;

    // name, speciesId, enclosureId, birthDate, weight, feedingIntervalHours, description
    private AnimalRequest request() {
        return new AnimalRequest("  Leo ", 1L, 2L, LocalDate.of(2020, 5, 1), 180.5f, 6, "A lion");
    }

    private Species lion() {
        return Species.builder().id(1L).name("Lion")
                .habitatType(HabitatType.SAVANNA).dietType(DietType.CARNIVORE).build();
    }

    private Enclosure savanna(HabitatType habitat) {
        return Enclosure.builder().id(2L).name("Savannah").habitatType(habitat).capacity(2).build();
    }

    @Test
    void findAll_returnsAnimalsSortedByName() {
        Animal animal = Animal.builder().id(5L).name("Leo").species(lion())
                .enclosure(savanna(HabitatType.SAVANNA)).feedingIntervalHours(6).build();
        when(animalRepository.findAll(Sort.by("name"))).thenReturn(List.of(animal));

        List<AnimalResponse> result = animalService.findAll();

        assertEquals(1, result.size());
        assertEquals("Lion", result.get(0).speciesName());
        assertEquals("Savannah", result.get(0).enclosureName());
        assertEquals(6, result.get(0).feedingIntervalHours());
    }

    @Test
    void findById_missingThrowsNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.findById(99L));
    }

    @Test
    void create_duplicateName_throwsAndSavesNothing() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(true);

        assertThrows(AnimalAlreadyExistsException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_unknownSpecies_throwsNotFound() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_unknownEnclosure_throwsNotFound() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_habitatMismatch_throwsAndSavesNothing() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.of(savanna(HabitatType.ARCTIC)));

        assertThrows(HabitatMismatchException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_fullEnclosure_throwsAndSavesNothing() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.of(savanna(HabitatType.SAVANNA)));
        when(animalRepository.countByEnclosureId(2L)).thenReturn(2L); // capacity is 2

        EnclosureFullException e = assertThrows(EnclosureFullException.class,
                () -> animalService.create(request()));

        assertEquals("Enclosure 'Savannah' is full", e.getMessage());
        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_lastFreePlace_isAccepted() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.of(savanna(HabitatType.SAVANNA)));
        when(animalRepository.countByEnclosureId(2L)).thenReturn(1L); // capacity 2, one place left
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = animalService.create(request());

        assertEquals("Leo", response.name());
    }

    @Test
    void create_valid_savesAnimalWithTrimmedNameAndAllFields() {
        Species species = lion();
        Enclosure enclosure = savanna(HabitatType.SAVANNA);
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(species));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.of(enclosure));
        when(animalRepository.countByEnclosureId(2L)).thenReturn(0L);
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));

        animalService.create(request());

        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(captor.capture());
        Animal saved = captor.getValue();
        assertEquals("Leo", saved.getName());
        assertEquals(species, saved.getSpecies());
        assertEquals(enclosure, saved.getEnclosure());
        assertEquals(LocalDate.of(2020, 5, 1), saved.getBirthDate());
        assertEquals(180.5f, saved.getWeight());
        assertEquals(6, saved.getFeedingIntervalHours());
        assertEquals("A lion", saved.getDescription());
        assertEquals(DeletedStatus.PRESENT, saved.getDeletedStatus());
    }
}