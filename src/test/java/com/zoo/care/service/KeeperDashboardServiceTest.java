package com.zoo.care.service;

import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.dto.KeeperEnclosureView;
import com.zoo.care.entity.*;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeeperDashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeeperAssignmentRepository keeperAssignmentRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @InjectMocks
    private KeeperDashboardService keeperDashboardService;

    private User keeper() {
        return User.builder().id(7L).username("ben").fullName("Ben Ten").role(Role.KEEPER).build();
    }

    private Enclosure enclosure(Long id, String name) {
        return Enclosure.builder().id(id).name(name).habitatType(HabitatType.SAVANNA).capacity(5).build();
    }

    private KeeperAssignment assignment(Enclosure enclosure) {
        return KeeperAssignment.builder().enclosure(enclosure).keeper(keeper())
                .startsAt(NOW.minusSeconds(3600)).build();
    }

    private Animal animal(Long id, String name, Enclosure enclosure) {
        Species lion = Species.builder().id(1L).name("Lion")
                .habitatType(HabitatType.SAVANNA).dietType(DietType.CARNIVORE).build();
        return Animal.builder().id(id).name(name).species(lion).enclosure(enclosure)
                .birthDate(LocalDate.of(2020, 5, 1)).weight(180.5f).feedingIntervalHours(6).build();
    }

    @Test
    void findForKeeper_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> keeperDashboardService.findForKeeper("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(keeperAssignmentRepository, animalRepository);
    }

    @Test
    void findForKeeper_noAssignments_returnsEmptyListWithoutLoadingAnimals() {
        when(userRepository.findByUsername("ben")).thenReturn(Optional.of(keeper()));
        when(keeperAssignmentRepository.findActiveByKeeper(7L, NOW)).thenReturn(List.of());

        List<KeeperEnclosureView> result = keeperDashboardService.findForKeeper("ben");

        assertThat(result).isEmpty();
        verifyNoInteractions(animalRepository);
    }

    @Test
    void findForKeeper_groupsAnimalsByEnclosureAndKeepsEnclosureOrder() {
        Enclosure forest = enclosure(1L, "Forest");
        Enclosure savanna = enclosure(2L, "Savannah");
        when(userRepository.findByUsername("ben")).thenReturn(Optional.of(keeper()));
        when(keeperAssignmentRepository.findActiveByKeeper(7L, NOW))
                .thenReturn(List.of(assignment(forest), assignment(savanna)));
        when(animalRepository.findByEnclosureIdInOrderByNameAsc(List.of(1L, 2L)))
                .thenReturn(List.of(animal(10L, "Leo", savanna), animal(11L, "Nala", savanna)));

        List<KeeperEnclosureView> result = keeperDashboardService.findForKeeper("ben");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).enclosure().name()).isEqualTo("Forest");
        assertThat(result.get(0).animals()).isEmpty();
        assertThat(result.get(1).enclosure().name()).isEqualTo("Savannah");
        assertThat(result.get(1).animals()).extracting(AnimalResponse::name).containsExactly("Leo", "Nala");
    }
}