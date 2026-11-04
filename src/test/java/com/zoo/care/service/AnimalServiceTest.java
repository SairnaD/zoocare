package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HealthStatus;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock AnimalRepository animalRepository;
    @Mock SpeciesRepository speciesRepository;
    @Mock EnclosureRepository enclosureRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks AnimalService animalService;

    private final MultipartFile photo =
            new MockMultipartFile("photo", "leo.png", "image/png", new byte[]{1, 2, 3});

    // fields in AnimalRequest order:
    // name, speciesId, enclosureId, birthDate, weight, size, feedingIntervalHours, description, photo
    private AnimalRequest request() {
        return new AnimalRequest("Leo", 1L, 2L, LocalDate.of(2020, 5, 1), 180.5f, 120, 6, "Lion", photo);
    }

    @Test
    void createAnimal_duplicateName_throwsAndStoresNothing() {
        when(animalRepository.existsByName("Leo")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> animalService.createAnimal(request()));

        verify(animalRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createAnimal_unknownSpecies_throwsAndStoresNothing() {
        when(animalRepository.existsByName("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> animalService.createAnimal(request()));

        verify(animalRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createAnimal_unknownEnclosure_throwsAndStoresNothing() {
        when(animalRepository.existsByName("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(Species.builder().id(1L).name("Lion").build()));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> animalService.createAnimal(request()));

        verify(animalRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createAnimal_valid_savesAnimalWithHoursAndStoredPhoto() {
        Species species = Species.builder().id(1L).name("Lion").build();
        Enclosure enclosure = Enclosure.builder().id(2L).name("Savannah").build();

        when(animalRepository.existsByName("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(species));
        when(enclosureRepository.findById(2L)).thenReturn(Optional.of(enclosure));
        when(fileStorageService.store(photo)).thenReturn("abc.png");
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));

        animalService.createAnimal(request());

        ArgumentCaptor<Animal> captor = ArgumentCaptor.forClass(Animal.class);
        verify(animalRepository).save(captor.capture());
        Animal saved = captor.getValue();

        assertEquals("Leo", saved.getName());
        assertEquals(species, saved.getSpecies());
        assertEquals(enclosure, saved.getEnclosure());
        assertEquals(Duration.ofHours(6), saved.getFeedingInterval());
        assertEquals("abc.png", saved.getPhoto());
        assertEquals(HealthStatus.HEALTHY, saved.getHealthStatus());
    }

    @Test
    void getAllAnimals_mapsNamesAndHealthStatus() {
        Species species = Species.builder().id(1L).name("Lion").build();
        Enclosure enclosure = Enclosure.builder().id(2L).name("Savannah").build();
        Animal animal = Animal.builder()
                .id(5L)
                .name("Leo")
                .species(species)
                .enclosure(enclosure)
                .feedingInterval(Duration.ofHours(6))
                .photo("abc.png")
                .healthStatus(HealthStatus.HEALTHY)
                .build();
        when(animalRepository.findAll()).thenReturn(List.of(animal));

        List<AnimalResponse> result = animalService.getAllAnimals();

        assertEquals(1, result.size());
        assertEquals("Lion", result.get(0).speciesName());
        assertEquals("Savannah", result.get(0).enclosureName());
        assertEquals("HEALTHY", result.get(0).healthStatus());
        assertEquals("abc.png", result.get(0).photo());
    }
}