package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.EnclosureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnclosureServiceTest {

    @Mock
    private EnclosureRepository enclosureRepository;

    @InjectMocks
    private EnclosureService enclosureService;

    @Test
    void create_trimsNameAndSavesEnclosure() {
        when(enclosureRepository.existsByNameIgnoreCase("Savanna 1")).thenReturn(false);
        when(enclosureRepository.save(any(Enclosure.class))).thenAnswer(invocation -> {
            Enclosure enclosure = invocation.getArgument(0);
            enclosure.setId(1L);
            return enclosure;
        });

        EnclosureResponse response = enclosureService.create(savannaRequest("  Savanna 1  "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Savanna 1");
        assertThat(response.habitatType()).isEqualTo(HabitatType.SAVANNA);
        assertThat(response.capacity()).isEqualTo(3);
    }

    @Test
    void create_duplicateName_throwsBusinessRuleException() {
        when(enclosureRepository.existsByNameIgnoreCase("Savanna 1")).thenReturn(true);

        assertThatThrownBy(() -> enclosureService.create(savannaRequest("Savanna 1")))
                .isInstanceOf(BusinessRuleException.class);
        verify(enclosureRepository, never()).save(any());
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_changesAllFields() {
        Enclosure savanna = existingSavanna();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(savanna));
        when(enclosureRepository.existsByNameIgnoreCaseAndIdNot("Penguin Pool", 1L)).thenReturn(false);

        EnclosureResponse response = enclosureService.update(1L,
                new EnclosureRequest("Penguin Pool", HabitatType.ARCTIC, 10));

        assertThat(response.name()).isEqualTo("Penguin Pool");
        assertThat(savanna.getHabitatType()).isEqualTo(HabitatType.ARCTIC);
        assertThat(savanna.getCapacity()).isEqualTo(10);
    }

    @Test
    void update_nameTakenByOtherEnclosure_throwsBusinessRuleException() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(enclosureRepository.existsByNameIgnoreCaseAndIdNot("Penguin Pool", 1L)).thenReturn(true);

        assertThatThrownBy(() -> enclosureService.update(1L, savannaRequest("Penguin Pool")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void delete_existingEnclosure_removesIt() {
        Enclosure savanna = existingSavanna();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(savanna));

        enclosureService.delete(1L);

        verify(enclosureRepository).delete(savanna);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(enclosureRepository, never()).delete(any());
    }



    private EnclosureRequest savannaRequest(String name) {
        return new EnclosureRequest(name, HabitatType.SAVANNA, 3);
    }

    private Enclosure existingSavanna() {
        return Enclosure.builder()
                .id(1L)
                .name("Savanna 1")
                .habitatType(HabitatType.SAVANNA)
                .capacity(3)
                .build();
    }
}