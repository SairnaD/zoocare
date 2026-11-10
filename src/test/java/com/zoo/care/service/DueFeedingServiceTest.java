package com.zoo.care.service;

import com.zoo.care.dto.DueFeedingItem;
import com.zoo.care.entity.*;
import com.zoo.care.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueFeedingServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Mock private UserRepository users;
    @Mock private KeeperAssignmentRepository assignments;
    @Mock private AnimalRepository animals;
    @Mock private FeedingRepository feedings;
    private KeeperDashboardService service;
    private final User keeper = User.builder().id(7L).role(Role.KEEPER).build();
    private final Enclosure enclosure = Enclosure.builder().id(2L).name("Savanna").build();

    @BeforeEach
    void setUp() {
        service = new KeeperDashboardService(users, assignments, Clock.fixed(NOW, ZoneOffset.UTC), animals, feedings);
    }

    @Test
    void neverFedAnimal_isReadyWithAllDisplayFields() {
        arrange(animal(1L, "Leo"));
        assertThat(service.findDueForKeeper("ben")).containsExactly(
                new DueFeedingItem(1L, "Leo", "Lion", "Savanna", 6, null, null));
        verify(feedings).findLastFeedingTimes(List.of(1L));
        verifyNoMoreInteractions(feedings);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void intervalBoundary_usesInclusiveNextAllowedTime(long nanosAfterBoundary) {
        arrange(animal(1L, "Leo"));
        Instant last = NOW.minusSeconds(21600).minusNanos(nanosAfterBoundary);
        when(feedings.findLastFeedingTimes(List.of(1L))).thenReturn(List.of(lastTime(1L, last)));
        List<DueFeedingItem> result = service.findDueForKeeper("ben");
        if (nanosAfterBoundary < 0) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().lastFedAt()).isEqualTo(last);
            assertThat(result.getFirst().nextAllowedAt()).isEqualTo(NOW.minusNanos(nanosAfterBoundary));
        }
    }

    @Test
    void currentInterval_isUsed() {
        Animal animal = animal(1L, "Leo");
        animal.setFeedingIntervalHours(8);
        arrange(animal);
        when(feedings.findLastFeedingTimes(List.of(1L)))
                .thenReturn(List.of(lastTime(1L, NOW.minusSeconds(21600))));
        assertThat(service.findDueForKeeper("ben")).isEmpty();
    }

    @Test
    void deletedAnimals_areExcludedBeforeHistoryQuery() {
        Animal deleted = animal(2L, "Deleted");
        deleted.setDeletedStatus(DeletedStatus.DELETED);
        arrange(animal(1L, "Leo"), deleted);
        assertThat(service.findDueForKeeper("ben")).extracting(DueFeedingItem::animalId).containsExactly(1L);
        verify(feedings).findLastFeedingTimes(List.of(1L));
    }

    @Test
    void noPresentAnimals_skipsHistoryQuery() {
        Animal deleted = animal(1L, "Deleted");
        deleted.setDeletedStatus(DeletedStatus.DELETED);
        arrange(deleted);
        assertThat(service.findDueForKeeper("ben")).isEmpty();
        verifyNoInteractions(feedings);
    }

    @Test
    void emptyEnclosure_skipsHistoryQuery() {
        arrange();
        assertThat(service.findDueForKeeper("ben")).isEmpty();
        verifyNoInteractions(feedings);
    }

    @Test
    void noAssignments_skipsAnimalAndHistoryQueries() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of());
        assertThat(service.findDueForKeeper("ben")).isEmpty();
        verifyNoInteractions(animals, feedings);
    }

    @Test
    void missingUser_isDeniedBeforeLoadingData() {
        when(users.findByUsername("ben")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findDueForKeeper("ben"))
                .isInstanceOf(AccessDeniedException.class).hasMessage("Current user not found");
        verifyNoInteractions(assignments, animals, feedings);
    }

    @Test
    void admin_isDeniedBeforeLoadingData() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(User.builder().role(Role.ADMIN).build()));
        assertThatThrownBy(() -> service.findDueForKeeper("ben")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assignments, animals, feedings);
    }

    @Test
    void duplicateAssignments_doNotDuplicateAnimalsOrEnclosureIds() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        KeeperAssignment assignment = KeeperAssignment.builder().enclosure(enclosure).build();
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment, assignment));
        when(animals.findByEnclosureIdInOrderByNameAsc(List.of(2L))).thenReturn(List.of(animal(1L, "Leo")));
        assertThat(service.findDueForKeeper("ben")).hasSize(1);
        verify(animals).findByEnclosureIdInOrderByNameAsc(List.of(2L));
    }

    @Test
    void sorting_putsNeverFedFirstThenOldestDueTimeThenNameAndId() {
        arrange(animal(6L, "Zulu"), animal(3L, "beta"), animal(2L, "Alpha"),
                animal(5L, "alpha"), animal(4L, "alpha"), animal(1L, "anna"));
        when(feedings.findLastFeedingTimes(List.of(6L, 3L, 2L, 5L, 4L, 1L))).thenReturn(List.of(
                lastTime(6L, NOW.minusSeconds(30000)), lastTime(3L, NOW.minusSeconds(21600)),
                lastTime(2L, NOW.minusSeconds(21600))));
        assertThat(service.findDueForKeeper("ben")).extracting(DueFeedingItem::animalId)
                .containsExactly(4L, 5L, 1L, 6L, 2L, 3L);
    }

    private void arrange(Animal... values) {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of(
                KeeperAssignment.builder().enclosure(enclosure).build()));
        when(animals.findByEnclosureIdInOrderByNameAsc(List.of(2L))).thenReturn(List.of(values));
    }

    private Animal animal(Long id, String name) {
        return Animal.builder().id(id).name(name).enclosure(enclosure)
                .species(Species.builder().name("Lion").build()).feedingIntervalHours(6)
                .deletedStatus(DeletedStatus.PRESENT).build();
    }

    private LastFeedingTime lastTime(Long id, Instant time) {
        return new LastFeedingTime() {
            public Long getAnimalId() { return id; }
            public Instant getLastFedAt() { return time; }
        };
    }
}
