package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.entity.*;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class EnclosureServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeeperAssignmentRepository keeperAssignmentRepository;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private User keeper() {
        return User.builder().id(7L).username("ben").fullName("Ben Ten").role(Role.KEEPER).build();
    }

    @Mock
    private EnclosureRepository enclosureRepository;

    @Mock
    private AnimalRepository animalRepository;

    @InjectMocks
    private EnclosureService enclosureService;


    @Test
    void switchKeeper_sameKeeper_rejectsWithoutChangingAssignment() {
        Enclosure enclosure = existingSavanna();
        User oldKeeper = keeper();
        Instant originalStart = NOW.minusSeconds(3600);

        KeeperAssignment oldAssignment = KeeperAssignment.builder()
                .id(10L)
                .enclosure(enclosure)
                .keeper(oldKeeper)
                .startsAt(originalStart)
                .createdAt(originalStart)
                .endsAt(null)
                .build();

        when(enclosureRepository.findById(1L))
                .thenReturn(Optional.of(enclosure));
        when(keeperAssignmentRepository.findById(10L))
                .thenReturn(Optional.of(oldAssignment));
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(oldKeeper));

        assertThatThrownBy(() ->
                enclosureService.switchKeeper(1L, 10L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("New keeper must be different from the current keeper");

        assertThat(oldAssignment.getEndsAt()).isNull();
        assertThat(oldAssignment.getKeeper()).isSameAs(oldKeeper);
        assertThat(oldAssignment.getEnclosure()).isSameAs(enclosure);
        assertThat(oldAssignment.getStartsAt()).isEqualTo(originalStart);
        assertThat(oldAssignment.getCreatedAt()).isEqualTo(originalStart);

        verify(keeperAssignmentRepository, never()).save(any());
    }


    @Test
    void switchKeeper_closesOldAssignmentAndCreatesNewOne() {
        Enclosure enclosure = existingSavanna();
        User oldKeeper = keeper();

        User newKeeper = User.builder()
                .id(8L)
                .username("amy")
                .fullName("Amy Adams")
                .role(Role.KEEPER)
                .build();

        Instant originalStart = NOW.minusSeconds(3600);

        KeeperAssignment oldAssignment = KeeperAssignment.builder()
                .id(10L)
                .enclosure(enclosure)
                .keeper(oldKeeper)
                .startsAt(originalStart)
                .createdAt(originalStart)
                .endsAt(null)
                .build();

        when(enclosureRepository.findById(1L))
                .thenReturn(Optional.of(enclosure));
        when(keeperAssignmentRepository.findById(10L))
                .thenReturn(Optional.of(oldAssignment));
        when(userRepository.findById(8L))
                .thenReturn(Optional.of(newKeeper));
        when(keeperAssignmentRepository.existsActiveAssignment(1L, 8L, NOW))
                .thenReturn(false);
        when(keeperAssignmentRepository.save(any(KeeperAssignment.class)))
                .thenAnswer(invocation -> {
                    KeeperAssignment assignment = invocation.getArgument(0);
                    assignment.setId(11L);
                    return assignment;
                });

        KeeperAssignmentResponse response =
                enclosureService.switchKeeper(1L, 10L, 8L);

        assertThat(oldAssignment.getEndsAt()).isEqualTo(NOW);
        assertThat(oldAssignment.getKeeper()).isSameAs(oldKeeper);
        assertThat(oldAssignment.getStartsAt()).isEqualTo(originalStart);
        assertThat(oldAssignment.getCreatedAt()).isEqualTo(originalStart);

        ArgumentCaptor<KeeperAssignment> captor =
                ArgumentCaptor.forClass(KeeperAssignment.class);

        verify(keeperAssignmentRepository).save(captor.capture());

        KeeperAssignment newAssignment = captor.getValue();

        assertThat(newAssignment).isNotSameAs(oldAssignment);
        assertThat(newAssignment.getEnclosure()).isSameAs(enclosure);
        assertThat(newAssignment.getKeeper()).isSameAs(newKeeper);
        assertThat(newAssignment.getStartsAt()).isEqualTo(NOW);
        assertThat(newAssignment.getCreatedAt()).isEqualTo(NOW);
        assertThat(newAssignment.getEndsAt()).isNull();

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.enclosureId()).isEqualTo(1L);
        assertThat(response.keeperId()).isEqualTo(8L);
        assertThat(response.keeperName()).isEqualTo("Amy Adams");
        assertThat(response.startsAt()).isEqualTo(NOW);
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.endsAt()).isNull();
    }


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
    void findAll_returnsEnclosuresSortedByName() {
        when(enclosureRepository.findAll(Sort.by("name"))).thenReturn(List.of(existingSavanna()));

        List<EnclosureResponse> result = enclosureService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Savanna 1");
    }

    @Test
    void findById_existingId_returnsEnclosure() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));

        EnclosureResponse response = enclosureService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.capacity()).isEqualTo(3);
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.update(99L, savannaRequest("Savanna 1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingEnclosure_removesIt() {
        Enclosure savanna = existingSavanna();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(savanna));

        enclosureService.delete(1L);

        verify(enclosureRepository).delete(savanna);
        verify(enclosureRepository).flush();
    }

    @Test
    void delete_enclosureWithAnimals_throwsBusinessRuleException() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        doThrow(new DataIntegrityViolationException("foreign key violation"))
                .when(enclosureRepository).flush();

        assertThatThrownBy(() -> enclosureService.delete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Enclosure still has animals and cannot be deleted");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(enclosureRepository, never()).delete(any());
    }

    @Test
    void delete_removesAssignmentsBeforeTheEnclosure() {
        Enclosure savanna = existingSavanna();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(savanna));

        enclosureService.delete(1L);

        InOrder order = inOrder(keeperAssignmentRepository, enclosureRepository);
        order.verify(keeperAssignmentRepository).deleteByEnclosureId(1L);
        order.verify(enclosureRepository).delete(savanna);
        order.verify(enclosureRepository).flush();
    }

    @Test
    void assignKeeper_createsAssignmentStartingNow() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(userRepository.findById(7L)).thenReturn(Optional.of(keeper()));
        when(keeperAssignmentRepository.existsActiveAssignment(1L, 7L, NOW)).thenReturn(false);
        when(keeperAssignmentRepository.save(any(KeeperAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KeeperAssignmentResponse response = enclosureService.assignKeeper(1L, 7L);

        assertThat(response.enclosureId()).isEqualTo(1L);
        assertThat(response.keeperId()).isEqualTo(7L);
        assertThat(response.startsAt()).isEqualTo(NOW);
        assertThat(response.endsAt()).isNull();
    }

    @Test
    void assignKeeper_userWithAdminRole_throwsBusinessRuleException() {
        User admin = User.builder().id(1L).username("admin").fullName("Admin").role(Role.ADMIN).build();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> enclosureService.assignKeeper(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(keeperAssignmentRepository, never()).save(any());
    }

    @Test
    void assignKeeper_unknownUser_throwsNotFound() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.assignKeeper(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignKeeper_unknownEnclosure_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.assignKeeper(99L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void assignKeeper_alreadyAssigned_throwsBusinessRuleException() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(userRepository.findById(7L)).thenReturn(Optional.of(keeper()));
        when(keeperAssignmentRepository.existsActiveAssignment(1L, 7L, NOW)).thenReturn(true);

        assertThatThrownBy(() -> enclosureService.assignKeeper(1L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Keeper is already assigned to this enclosure");
        verify(keeperAssignmentRepository, never()).save(any());
    }

    @Test
    void unassignKeeper_setsEndTimeToNow() {
        KeeperAssignment assignment = KeeperAssignment.builder()
                .id(10L).enclosure(existingSavanna()).keeper(keeper())
                .startsAt(NOW.minusSeconds(3600)).createdAt(NOW.minusSeconds(3600)).build();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(keeperAssignmentRepository.findActive(1L, 7L, NOW)).thenReturn(Optional.of(assignment));

        enclosureService.unassignKeeper(1L, 7L);

        assertThat(assignment.getEndsAt()).isEqualTo(NOW);
    }

    @Test
    void unassignKeeper_notAssigned_throwsNotFound() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(keeperAssignmentRepository.findActive(1L, 7L, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.unassignKeeper(1L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_habitatChangeWithAnimals_throwsBusinessRuleException() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(enclosureRepository.existsByNameIgnoreCaseAndIdNot("Savanna 1", 1L)).thenReturn(false);
        when(animalRepository.existsByEnclosureId(1L)).thenReturn(true);

        assertThatThrownBy(() -> enclosureService.update(1L,
                new EnclosureRequest("Savanna 1", HabitatType.ARCTIC, 3)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_capacityBelowAnimalCount_throwsBusinessRuleException() {
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(enclosureRepository.existsByNameIgnoreCaseAndIdNot("Savanna 1", 1L)).thenReturn(false);
        when(animalRepository.countByEnclosureId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> enclosureService.update(1L,
                new EnclosureRequest("Savanna 1", HabitatType.SAVANNA, 2)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findActiveKeepers_returnsActiveAssignments() {
        KeeperAssignment assignment = KeeperAssignment.builder()
                .id(10L).enclosure(existingSavanna()).keeper(keeper())
                .startsAt(NOW.minusSeconds(3600)).createdAt(NOW.minusSeconds(3600)).build();
        when(enclosureRepository.findById(1L)).thenReturn(Optional.of(existingSavanna()));
        when(keeperAssignmentRepository.findActiveByEnclosure(1L, NOW)).thenReturn(List.of(assignment));

        assertThat(enclosureService.findActiveKeepers(1L)).hasSize(1);
    }

    @Test
    void findActiveKeepers_unknownEnclosure_throwsNotFound() {
        when(enclosureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enclosureService.findActiveKeepers(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllKeepers_returnsAllKeepers() {
        User amy = User.builder().id(8L).username("amy").fullName("Amy Adams").role(Role.KEEPER).build();
        when(userRepository.findAllByRoleOrderByFullNameAsc(Role.KEEPER)).thenReturn(List.of(amy, keeper()));

        assertThat(enclosureService.findAllKeepers()).hasSize(2);
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