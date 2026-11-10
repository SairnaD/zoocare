package com.zoo.care.service;

import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.exception.SpeciesAlreadyExistsException;
import com.zoo.care.exception.SpeciesHabitatLockedException;
import com.zoo.care.exception.SpeciesInUseException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpeciesServiceTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private AnimalRepository animalRepository;

    @InjectMocks
    private SpeciesService speciesService;

    @Test
    void create_trimsNameAndSavesSpecies() {
        when(speciesRepository.existsByNameIgnoreCase("Lion")).thenReturn(false);
        when(speciesRepository.save(any(Species.class))).thenAnswer(invocation -> {
            Species species = invocation.getArgument(0);
            species.setId(1L);
            return species;
        });

        SpeciesResponse response = speciesService.create(lionRequest("  Lion  "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Lion");
        assertThat(response.habitatType()).isEqualTo(HabitatType.SAVANNA);
        assertThat(response.dietType()).isEqualTo(DietType.CARNIVORE);
    }

    @Test
    void create_duplicateName_throwsBusinessRuleException() {
        when(speciesRepository.existsByNameIgnoreCase("Lion")).thenReturn(true);

        assertThatThrownBy(() -> speciesService.create(lionRequest("Lion")))
                .isInstanceOf(SpeciesAlreadyExistsException.class).hasMessage("Species with name 'Lion' already exists");
        verify(speciesRepository, never()).save(any());
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        when(speciesRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesAllFields() {
        Species lion = existingLion();
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion));
        when(speciesRepository.existsByNameIgnoreCaseAndIdNot("Tiger", 1L)).thenReturn(false);

        SpeciesResponse response = speciesService.update(1L,
                new SpeciesRequest("Tiger", HabitatType.JUNGLE, DietType.CARNIVORE));

        assertThat(response.name()).isEqualTo("Tiger");
        assertThat(lion.getName()).isEqualTo("Tiger");
        assertThat(lion.getHabitatType()).isEqualTo(HabitatType.JUNGLE);
    }

    @Test
    void update_nameTakenByOtherSpecies_throwsBusinessRuleException() {
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(existingLion()));
        when(speciesRepository.existsByNameIgnoreCaseAndIdNot("Tiger", 1L)).thenReturn(true);

        assertThatThrownBy(() -> speciesService.update(1L, lionRequest("Tiger")))
                .isInstanceOf(SpeciesAlreadyExistsException.class);
    }

    @Test
    void delete_existingSpecies_removesIt() {
        Species lion = existingLion();
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion));

        speciesService.delete(1L);

        verify(speciesRepository).delete(lion);
        verify(speciesRepository).flush();
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(speciesRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> speciesService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(speciesRepository, never()).delete(any());
    }

    @Test
    void delete_speciesInUse_throwsBusinessRuleException() {
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(existingLion()));
        doThrow(new DataIntegrityViolationException("foreign key violation"))
                .when(speciesRepository).flush();

        assertThatThrownBy(() -> speciesService.delete(1L))
                .isInstanceOf(SpeciesInUseException.class)
                .hasMessage("Species is in use and cannot be deleted");
    }

    @Test
    void update_habitatChangeWithAnimals_throwsBusinessRuleException() {
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(existingLion()));
        when(speciesRepository.existsByNameIgnoreCaseAndIdNot("Lion", 1L)).thenReturn(false);
        when(animalRepository.existsBySpeciesId(1L)).thenReturn(true);

        assertThatThrownBy(() -> speciesService.update(1L,
                new SpeciesRequest("Lion", HabitatType.JUNGLE, DietType.CARNIVORE)))
                .isInstanceOf(SpeciesHabitatLockedException.class);
    }

    private SpeciesRequest lionRequest(String name) {
        return new SpeciesRequest(name, HabitatType.SAVANNA, DietType.CARNIVORE);
    }

    private Species existingLion() {
        return Species.builder()
                .id(1L)
                .name("Lion")
                .habitatType(HabitatType.SAVANNA)
                .dietType(DietType.CARNIVORE)
                .build();
    }
}