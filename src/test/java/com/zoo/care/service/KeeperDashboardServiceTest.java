package com.zoo.care.service;

import com.zoo.care.dto.KeeperAnimalView;
import com.zoo.care.entity.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.FeedingRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
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

    @Mock private UserRepository users;
    @Mock private KeeperAssignmentRepository assignments;
    @Mock private AnimalRepository animals;
    @Mock private FeedingRepository feedings;
    @Spy private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    @InjectMocks private KeeperDashboardService service;

    private final User keeper = User.builder().id(7L).username("ben").fullName("Ben Ten").role(Role.KEEPER).build();

    @Test
    void neverFedAnimal_needsFeedingAndHasAllDisplayFields() {
        Animal leo = animal(1L, "Leo", 6);
        givenAnimals(leo);
        when(feedings.findLatestByAnimalIds(List.of(1L))).thenReturn(List.of());

        assertThat(service.findAnimalsForKeeper("ben")).containsExactly(new KeeperAnimalView(
                1L, "Leo", "Lion", "Savanna 1", HabitatType.SAVANNA, LocalDate.of(2020, 5, 1), 6,
                180.5f, "Calm lion", 6, null, null, null, FeedingStatus.NEED_TO_FEED));
    }

    @ParameterizedTest
    @CsvSource({
            "360, NEED_TO_FEED",  // next feeding exactly now
            "330, FEED_SOON",     // next feeding in 30 minutes
            "300, FEED_SOON",     // next feeding in exactly 1 hour
            "299, FED"            // next feeding in 61 minutes
    })
    void status_dependsOnTimeUntilNextFeeding(long minutesSinceLastFeeding, FeedingStatus expected) {
        Animal leo = animal(1L, "Leo", 6);
        givenAnimals(leo);
        Instant lastFedAt = NOW.minusSeconds(minutesSinceLastFeeding * 60);
        when(feedings.findLatestByAnimalIds(List.of(1L))).thenReturn(List.of(feeding(10L, leo, lastFedAt)));

        KeeperAnimalView view = service.findAnimalsForKeeper("ben").get(0);

        assertThat(view.status()).isEqualTo(expected);
        assertThat(view.lastFedAt()).isEqualTo(lastFedAt);
        assertThat(view.lastFedBy()).isEqualTo("Ann Lee");
        assertThat(view.nextFeedingAt()).isEqualTo(lastFedAt.plusSeconds(6 * 3600));
    }

    @Test
    void twoFeedingsAtTheSameTime_useTheOneWithHigherId() {
        Animal leo = animal(1L, "Leo", 6);
        givenAnimals(leo);
        Feedings first = feeding(10L, leo, NOW.minusSeconds(3600));
        Feedings second = feeding(11L, leo, NOW.minusSeconds(3600));
        second.setRecordedBy(User.builder().id(9L).fullName("Janis Kalnins").build());
        when(feedings.findLatestByAnimalIds(List.of(1L))).thenReturn(List.of(first, second));

        assertThat(service.findAnimalsForKeeper("ben")).singleElement()
                .extracting(KeeperAnimalView::lastFedBy).isEqualTo("Janis Kalnins");
    }

    @Test
    void animals_areSortedByStatusThenNextFeedingThenName() {
        Animal fed = animal(1L, "Alpha", 6);
        Animal overdue = animal(2L, "Bravo", 6);
        Animal neverFed = animal(3L, "Charlie", 6);
        Animal soon = animal(4L, "Delta", 6);
        givenAnimals(fed, overdue, neverFed, soon);
        when(feedings.findLatestByAnimalIds(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
                feeding(10L, fed, NOW.minusSeconds(3600)),
                feeding(11L, overdue, NOW.minusSeconds(8 * 3600)),
                feeding(12L, soon, NOW.minusSeconds(330 * 60))));

        assertThat(service.findAnimalsForKeeper("ben")).extracting(KeeperAnimalView::animalName)
                .containsExactly("Charlie", "Bravo", "Delta", "Alpha");
    }

    @Test
    void deletedAnimals_areExcludedBeforeHistoryQuery() {
        Animal leo = animal(1L, "Leo", 6);
        Animal gone = animal(2L, "Gone", 6);
        gone.setDeletedStatus(DeletedStatus.DELETED);
        givenAnimals(leo, gone);
        when(feedings.findLatestByAnimalIds(List.of(1L))).thenReturn(List.of());

        assertThat(service.findAnimalsForKeeper("ben")).extracting(KeeperAnimalView::animalId).containsExactly(1L);
        verify(feedings).findLatestByAnimalIds(List.of(1L));
    }

    @Test
    void onlyDeletedAnimals_skipHistoryQuery() {
        Animal gone = animal(2L, "Gone", 6);
        gone.setDeletedStatus(DeletedStatus.DELETED);
        givenAnimals(gone);

        assertThat(service.findAnimalsForKeeper("ben")).isEmpty();
        verifyNoInteractions(feedings);
    }

    @Test
    void noAssignments_skipAnimalAndHistoryQueries() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of());

        assertThat(service.findAnimalsForKeeper("ben")).isEmpty();
        verifyNoInteractions(animals, feedings);
    }

    @Test
    void duplicateAssignments_doNotDuplicateEnclosureIds() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment(), assignment()));
        when(animals.findByEnclosureIdInOrderByNameAsc(List.of(2L))).thenReturn(List.of());

        assertThat(service.findAnimalsForKeeper("ben")).isEmpty();
        verify(animals).findByEnclosureIdInOrderByNameAsc(List.of(2L));
    }

    @Test
    void missingUser_isDeniedBeforeLoadingData() {
        when(users.findByUsername("ben")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAnimalsForKeeper("ben"))
                .isInstanceOf(AccessDeniedException.class).hasMessage("Current user not found");
        verifyNoInteractions(assignments, animals, feedings);
    }

    @Test
    void admin_isDeniedBeforeLoadingData() {
        keeper.setRole(Role.ADMIN);
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));

        assertThatThrownBy(() -> service.findAnimalsForKeeper("ben")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assignments, animals, feedings);
    }

    private void givenAnimals(Animal... list) {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment()));
        when(animals.findByEnclosureIdInOrderByNameAsc(List.of(2L))).thenReturn(List.of(list));
    }

    private KeeperAssignment assignment() {
        return KeeperAssignment.builder().enclosure(Enclosure.builder().id(2L).build()).build();
    }

    private Animal animal(Long id, String name, int intervalHours) {
        return Animal.builder().id(id).name(name)
                .species(Species.builder().name("Lion").build())
                .enclosure(Enclosure.builder().id(2L).name("Savanna 1").habitatType(HabitatType.SAVANNA).build())
                .birthDate(LocalDate.of(2020, 5, 1)).weight(180.5f).description("Calm lion")
                .feedingIntervalHours(intervalHours).deletedStatus(DeletedStatus.PRESENT).build();
    }

    private Feedings feeding(Long id, Animal animal, Instant fedAt) {
        return Feedings.builder().id(id).animal(animal)
                .recordedBy(User.builder().id(8L).fullName("Ann Lee").build())
                .food("Hay").amountKg(BigDecimal.ONE).fedAt(fedAt).build();
    }
}