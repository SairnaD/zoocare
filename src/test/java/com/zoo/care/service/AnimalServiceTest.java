package com.zoo.care.service;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.Species;
import com.zoo.care.exception.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private AnimalRequest updateRequest(String name, Long speciesId, Long enclosureId) {
        return new AnimalRequest(name, speciesId, enclosureId, LocalDate.of(2021, 6, 2), 190.0f, 8, "Updated");
    }

    private Species lion() {
        return species(1L, "Lion", HabitatType.SAVANNA);
    }

    private Species species(Long id, String name, HabitatType habitat) {
        return Species.builder().id(id).name(name).habitatType(habitat).dietType(DietType.CARNIVORE).build();
    }

    /** Enclosure 2 "Savannah" with room for two animals. */
    private Enclosure savanna(HabitatType habitat) {
        return enclosure(2L, "Savannah", habitat, 2);
    }

    private Enclosure enclosure(Long id, String name, HabitatType habitat, int capacity) {
        return Enclosure.builder().id(id).name(name).habitatType(habitat).capacity(capacity).build();
    }

    private Animal animal(Long id, String name, Species species, Enclosure enclosure) {
        return Animal.builder()
                .id(id)
                .name(name)
                .species(species)
                .enclosure(enclosure)
                .birthDate(LocalDate.of(2020, 5, 1))
                .weight(180.5f)
                .feedingIntervalHours(6)
                .description("A lion")
                .deletedStatus(DeletedStatus.PRESENT)
                .build();
    }

    private DeletedStatus aNonPresentStatus() {
        return Arrays.stream(DeletedStatus.values())
                .filter(s -> s != DeletedStatus.PRESENT).findFirst().orElseThrow();
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
    void findById_existing_returnsResponse() {
        when(animalRepository.findById(5L))
                .thenReturn(Optional.of(animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA))));

        AnimalResponse result = animalService.findById(5L);

        assertEquals("Leo", result.name());
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
        verifyNoInteractions(speciesRepository, enclosureRepository);
    }

    @Test
    void create_unknownSpecies_throwsNotFound() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
        verifyNoInteractions(enclosureRepository);
    }

    @Test
    void create_unknownEnclosure_throwsNotFound() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_habitatMismatch_throwsAndSavesNothing() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(savanna(HabitatType.ARCTIC)));

        assertThrows(HabitatMismatchException.class, () -> animalService.create(request()));

        verify(animalRepository, never()).save(any());
    }

    @Test
    void create_fullEnclosure_throwsAndSavesNothing() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(savanna(HabitatType.SAVANNA)));
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
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(savanna(HabitatType.SAVANNA)));
        when(animalRepository.countByEnclosureId(2L)).thenReturn(1L); // capacity 2, one place left
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));

        AnimalResponse response = animalService.create(request());

        assertEquals("Leo", response.name());
    }

    @Test
    void create_valid_savesTrimmedAnimalWithDefaultsAndLocksTheEnclosure() {
        Species species = lion();
        Enclosure enclosure = savanna(HabitatType.SAVANNA);
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(species));
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(enclosure));
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
        verify(animalRepository).flush();
        verify(enclosureRepository, never()).findById(any());
    }

    @Test
    void create_concurrentDuplicateName_isReportedAsAnimalAlreadyExistsViolation() {
        when(animalRepository.existsByNameIgnoreCase("Leo")).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(savanna(HabitatType.SAVANNA)));
        when(animalRepository.save(any(Animal.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DataIntegrityViolationException("duplicate key")).when(animalRepository).flush();

        assertThrows(AnimalAlreadyExistsException.class, () -> animalService.create(request()));
    }

    @Test
    void update_sameEnclosure_succeedsEvenWhenTheEnclosureIsFull() {
        Enclosure full = enclosure(2L, "Savannah", HabitatType.SAVANNA, 1);   // 1 animal, capacity 1
        Animal leo = animal(5L, "Leo", lion(), full);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Leo", 5L)).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));

        animalService.update(5L, updateRequest("Leo", 1L, 2L));

        assertEquals("Updated", leo.getDescription());
        assertEquals(8, leo.getFeedingIntervalHours());
        verify(animalRepository, never()).countByEnclosureId(any());
        verify(enclosureRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void update_toAnotherEnclosureWithSpace_movesTheAnimal() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        Enclosure savanna2 = enclosure(3L, "Savannah 2", HabitatType.SAVANNA, 5);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Leo", 5L)).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(savanna2));
        when(animalRepository.countByEnclosureId(3L)).thenReturn(1L);

        animalService.update(5L, updateRequest("Leo", 1L, 3L));

        assertSame(savanna2, leo.getEnclosure());
    }

    @Test
    void update_toAFullEnclosure_throwsAndLeavesTheAnimalWhereItWas() {
        Enclosure original = savanna(HabitatType.SAVANNA);
        Animal leo = animal(5L, "Leo", lion(), original);
        Enclosure savanna2 = enclosure(3L, "Savannah 2", HabitatType.SAVANNA, 5);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Leo", 5L)).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(savanna2));
        when(animalRepository.countByEnclosureId(3L)).thenReturn(5L);

        assertThrows(EnclosureFullException.class,
                () -> animalService.update(5L, updateRequest("Leo", 1L, 3L)));

        assertSame(original, leo.getEnclosure());
    }

    @Test
    void update_speciesWithWrongHabitatForTheCurrentEnclosure_throws() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        Species penguin = species(4L, "Penguin", HabitatType.ARCTIC);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Leo", 5L)).thenReturn(false);
        when(speciesRepository.findById(4L)).thenReturn(Optional.of(penguin));

        assertThrows(HabitatMismatchException.class,
                () -> animalService.update(5L, updateRequest("Leo", 4L, 2L)));

        verify(animalRepository, never()).countByEnclosureId(any());
    }

    @Test
    void update_nameTakenByAnotherAnimal_throwsBeforeLoadingSpeciesOrEnclosure() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Simba", 5L)).thenReturn(true);

        assertThrows(AnimalAlreadyExistsException.class,
                () -> animalService.update(5L, updateRequest("Simba", 1L, 2L)));

        verifyNoInteractions(speciesRepository, enclosureRepository);
    }

    @Test
    void update_unknownAnimal_throwsNotFound() {
        when(animalRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> animalService.update(5L, updateRequest("Leo", 1L, 2L)));
    }

    @Test
    void update_animalNoLongerInTheZoo_throws() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        leo.setDeletedStatus(aNonPresentStatus());
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));

        assertThrows(AnimalDeletedException.class,
                () -> animalService.update(5L, updateRequest("Leo", 1L, 2L)));

        verifyNoInteractions(speciesRepository, enclosureRepository);
    }

    @Test
    void update_concurrentDuplicateName_isReportedAsAnimalAlreadyExistsViolation() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(animalRepository.existsByNameIgnoreCaseAndIdNot("Leo", 5L)).thenReturn(false);
        when(speciesRepository.findById(1L)).thenReturn(Optional.of(lion()));
        doThrow(new DataIntegrityViolationException("duplicate key")).when(animalRepository).flush();

        assertThrows(AnimalAlreadyExistsException.class,
                () -> animalService.update(5L, updateRequest("Leo", 1L, 2L)));
    }

    @Test
    void transfer_valid_movesTheAnimalAndReturnsTheNewEnclosure() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        Enclosure savanna2 = enclosure(3L, "Savannah 2", HabitatType.SAVANNA, 5);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(savanna2));
        when(animalRepository.countByEnclosureId(3L)).thenReturn(1L);

        AnimalResponse result = animalService.transferAnimal(5L, 3L);

        assertSame(savanna2, leo.getEnclosure());
        assertEquals("Savannah 2", result.enclosureName());
    }

    @Test
    void transfer_locksTheTargetBeforeCountingItsOccupants() {
        Enclosure savanna2 = enclosure(3L, "Savannah 2", HabitatType.SAVANNA, 5);
        when(animalRepository.findById(5L))
                .thenReturn(Optional.of(animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA))));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(savanna2));
        when(animalRepository.countByEnclosureId(3L)).thenReturn(0L);

        animalService.transferAnimal(5L, 3L);

        InOrder order = inOrder(enclosureRepository, animalRepository);
        order.verify(enclosureRepository).findByIdForUpdate(3L);
        order.verify(animalRepository).countByEnclosureId(3L);
    }

    @Test
    void transfer_toTheSameEnclosure_throwsWithoutLocking() {
        when(animalRepository.findById(5L))
                .thenReturn(Optional.of(animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA))));

        assertThrows(SameEnclosureException.class, () -> animalService.transferAnimal(5L, 2L));

        verifyNoInteractions(enclosureRepository);
    }

    @Test
    void transfer_toAFullEnclosure_throwsAndDoesNotMove() {
        Enclosure original = savanna(HabitatType.SAVANNA);
        Animal leo = animal(5L, "Leo", lion(), original);
        Enclosure savanna2 = enclosure(3L, "Savannah 2", HabitatType.SAVANNA, 5);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(savanna2));
        when(animalRepository.countByEnclosureId(3L)).thenReturn(5L);

        assertThrows(EnclosureFullException.class, () -> animalService.transferAnimal(5L, 3L));

        assertSame(original, leo.getEnclosure());
    }

    @Test
    void transfer_toAnEnclosureWithTheWrongHabitat_throwsAndDoesNotMove() {
        Enclosure original = savanna(HabitatType.SAVANNA);
        Animal leo = animal(5L, "Leo", lion(), original);
        Enclosure arctic = enclosure(3L, "Arctic House", HabitatType.ARCTIC, 5);
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        when(enclosureRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(arctic));

        assertThrows(HabitatMismatchException.class, () -> animalService.transferAnimal(5L, 3L));

        assertSame(original, leo.getEnclosure());
    }

    @Test
    void transfer_unknownTargetEnclosure_throwsNotFound() {
        when(animalRepository.findById(5L))
                .thenReturn(Optional.of(animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA))));
        when(enclosureRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.transferAnimal(5L, 9L));
    }

    @Test
    void delete_removesTheAnimalAndFlushes() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));

        animalService.delete(5L);

        InOrder order = inOrder(animalRepository);
        order.verify(animalRepository).delete(leo);
        order.verify(animalRepository).flush();
    }

    @Test
    void delete_unknownAnimal_throwsNotFound() {
        when(animalRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> animalService.delete(5L));

        verify(animalRepository, never()).delete(any());
    }

    @Test
    void delete_animalWithRelatedRecords_throwsAnimalInUse() {
        Animal leo = animal(5L, "Leo", lion(), savanna(HabitatType.SAVANNA));
        when(animalRepository.findById(5L)).thenReturn(Optional.of(leo));
        doThrow(new DataIntegrityViolationException("foreign key violation"))
                .when(animalRepository).flush();

        assertThrows(AnimalInUseException.class, () -> animalService.delete(5L));
    }
}