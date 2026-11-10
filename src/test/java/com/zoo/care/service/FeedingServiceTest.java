package com.zoo.care.service;

import com.zoo.care.dto.AnimalFeedingGroup;
import com.zoo.care.dto.FeedingHistoryResponse;
import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.FeedingResponse;
import com.zoo.care.entity.*;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedingServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Mock private AnimalRepository animals;
    @Mock private FeedingRepository feedings;
    @Mock private UserRepository users;
    @Mock private KeeperAssignmentRepository assignments;
    private FeedingService service;
    private User keeper;
    private Animal animal;

    @BeforeEach
    void setUp() {
        service = new FeedingService(animals, feedings, users, assignments,
                Clock.fixed(NOW, ZoneOffset.UTC));
        keeper = User.builder().id(7L).username("ben").role(Role.KEEPER).build();
        animal = Animal.builder().id(5L)
                .enclosure(Enclosure.builder().id(2L).build())
                .feedingIntervalHours(6).deletedStatus(DeletedStatus.PRESENT).build();
    }

    @Test
    void firstFeeding_savesNormalizedDataAndReturnsServerValues() {
        allowAccess();
        when(feedings.findFirstByAnimalIdOrderByFedAtDescIdDesc(5L)).thenReturn(Optional.empty());
        returnSavedFeeding();

        FeedingResponse response = service.recordFeeding(5L,
                new FeedingRequest("  Hay  ", new BigDecimal("0.750"), "  Ate normally  "), "ben");

        ArgumentCaptor<Feedings> captor = ArgumentCaptor.forClass(Feedings.class);
        verify(feedings).save(captor.capture());
        Feedings saved = captor.getValue();
        assertThat(saved.getAnimal()).isSameAs(animal);
        assertThat(saved.getRecordedBy()).isSameAs(keeper);
        assertThat(saved.getFood()).isEqualTo("Hay");
        assertThat(saved.getAmountKg()).isEqualByComparingTo("0.750");
        assertThat(saved.getFedAt()).isEqualTo(NOW);
        assertThat(saved.getNote()).isEqualTo("Ate normally");
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.animalId()).isEqualTo(5L);
        assertThat(response.recordedById()).isEqualTo(7L);
        assertThat(response.food()).isEqualTo("Hay");
        assertThat(response.amountKg()).isEqualByComparingTo("0.750");
        assertThat(response.fedAt()).isEqualTo(NOW);
        assertThat(response.note()).isEqualTo("Ate normally");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingOrBlankNote_isStoredAsNull(String note) {
        allowAccess();
        returnSavedFeeding();
        FeedingResponse response = service.recordFeeding(5L,
                new FeedingRequest("Hay", new BigDecimal("0.750"), note), "ben");
        assertThat(response.note()).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {21600, 21601})
    void feedingAtOrAfterBoundary_isAllowed(long secondsSinceLastFeeding) {
        allowAccess();
        previousFeeding(NOW.minusSeconds(secondsSinceLastFeeding));
        returnSavedFeeding();
        assertThat(service.recordFeeding(5L, request(), "ben").fedAt()).isEqualTo(NOW);
        verify(feedings).save(any(Feedings.class));
    }

    @Test
    void feedingOneNanosecondBeforeBoundary_isRejectedWithoutChangingHistory() {
        allowAccess();
        Feedings previous = previousFeeding(NOW.minusSeconds(21600).plusNanos(1));
        Instant originalTime = previous.getFedAt();
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Animal can be fed again at " + NOW.plusNanos(1));
        verify(feedings, never()).save(any());
        assertThat(previous.getFedAt()).isEqualTo(originalTime);
        assertThat(previous.getRecordedBy().getId()).isEqualTo(8L);
        assertThat(animal.getDeletedStatus()).isEqualTo(DeletedStatus.PRESENT);
    }

    @Test
    void recentFeedingByAnotherKeeper_alsoPreventsFeeding() {
        allowAccess();
        previousFeeding(NOW.minusSeconds(3600));
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(NOW.plusSeconds(18000).toString());
        verify(feedings, never()).save(any());
    }

    @Test
    void currentAnimalInterval_isUsed() {
        allowAccess();
        animal.setFeedingIntervalHours(8);
        previousFeeding(NOW.minusSeconds(21600));
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(NOW.plusSeconds(7200).toString());
        verify(feedings, never()).save(any());
    }

    @Test
    void keeperWithoutActiveAssignment_isDeniedBeforeReadingHistory() {
        loadUserAndAnimal();
        when(assignments.existsActiveAssignment(2L, 7L, NOW)).thenReturn(false);
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(feedings);
    }

    @Test
    void admin_isDeniedEvenWithoutSpringProxy() {
        loadUserAndAnimal();
        keeper.setRole(Role.ADMIN);
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(assignments, feedings);
    }

    @Test
    void missingCurrentUser_isDeniedWithoutLoadingAnimal() {
        when(users.findByUsername("ben")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(AccessDeniedException.class).hasMessage("Current user not found");
        verifyNoInteractions(animals, assignments, feedings);
    }

    @Test
    void missingAnimal_isNotFoundAndNothingIsSaved() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(animals.findByIdForUpdate(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Animal with id 5 not found");
        verifyNoInteractions(assignments, feedings);
    }

    @Test
    void deletedAnimal_isRejectedForAnOtherwiseAuthorizedKeeper() {
        allowAccess();
        animal.setDeletedStatus(DeletedStatus.DELETED);
        assertThatThrownBy(() -> service.recordFeeding(5L, request(), "ben"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot record a feeding for a deleted animal");
        verifyNoInteractions(feedings);
        assertThat(animal.getDeletedStatus()).isEqualTo(DeletedStatus.DELETED);
    }

    @Test
    void serverTime_isReadAfterAnimalLockHasBeenAcquired() {
        Clock clock = mock(Clock.class);
        service = new FeedingService(animals, feedings, users, assignments, clock);
        allowAccess();
        when(clock.instant()).thenReturn(NOW);
        returnSavedFeeding();
        service.recordFeeding(5L, request(), "ben");
        var order = inOrder(animals, clock, assignments, feedings);
        order.verify(animals).findByIdForUpdate(5L);
        order.verify(clock).instant();
        order.verify(assignments).existsActiveAssignment(2L, 7L, NOW);
        order.verify(feedings).findFirstByAnimalIdOrderByFedAtDescIdDesc(5L);
        order.verify(feedings).save(any(Feedings.class));
        verify(clock, times(1)).instant();
    }

    @Test
    void admin_overviewReadsAllFeedingsOfLast7DaysWithoutAssignmentCheck() {
        keeper.setRole(Role.ADMIN);
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(feedings.findByFedAtGreaterThanEqualOrderByFedAtDescIdDesc(NOW.minus(7, ChronoUnit.DAYS)))
                .thenReturn(List.of());

        assertThat(service.getFeedingOverview("ben")).isEmpty();
        verifyNoInteractions(assignments);
    }

    @Test
    void keeper_overviewReadsFeedingsOfEnclosuresFromActiveAssignments() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment(2L), assignment(3L)));
        Feedings feeding = overviewFeeding(10L, namedAnimal(5L, "Zuri"), NOW.minusSeconds(3600));
        when(feedings.findByAnimalEnclosureIdInAndFedAtGreaterThanEqualOrderByFedAtDescIdDesc(
                List.of(2L, 3L), NOW.minus(7, ChronoUnit.DAYS))).thenReturn(List.of(feeding));

        List<AnimalFeedingGroup> overview = service.getFeedingOverview("ben");

        assertThat(overview).containsExactly(new AnimalFeedingGroup(
                new FeedingHistoryResponse(10L, "Zuri", "Giraffe", "Savanna 2", NOW.minusSeconds(3600),
                        "Hay", BigDecimal.ONE, "Ate all the hay", "Ann Lee"),
                List.of()));
    }

    @Test
    void keeperWithoutAssignments_getsEmptyOverviewWithoutReadingFeedings() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(assignments.findActiveByKeeper(7L, NOW)).thenReturn(List.of());

        assertThat(service.getFeedingOverview("ben")).isEmpty();
        verifyNoInteractions(feedings);
    }

    @Test
    void overview_groupsFeedingsByAnimalKeepingNewestFirst() {
        keeper.setRole(Role.ADMIN);
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        Animal zuri = namedAnimal(5L, "Zuri");
        Animal nala = namedAnimal(6L, "Nala");
        when(feedings.findByFedAtGreaterThanEqualOrderByFedAtDescIdDesc(NOW.minus(7, ChronoUnit.DAYS)))
                .thenReturn(List.of(
                        overviewFeeding(1L, zuri, NOW.minusSeconds(3600)),
                        overviewFeeding(2L, nala, NOW.minusSeconds(7200)),
                        overviewFeeding(3L, zuri, NOW.minusSeconds(10800)),
                        overviewFeeding(4L, nala, NOW.minusSeconds(14400)),
                        overviewFeeding(5L, zuri, NOW.minusSeconds(18000))));

        List<AnimalFeedingGroup> overview = service.getFeedingOverview("ben");

        assertThat(overview).hasSize(2);
        assertThat(overview.get(0).latest().id()).isEqualTo(1L);
        assertThat(overview.get(0).earlier()).extracting(FeedingHistoryResponse::id).containsExactly(3L, 5L);
        assertThat(overview.get(1).latest().id()).isEqualTo(2L);
        assertThat(overview.get(1).earlier()).extracting(FeedingHistoryResponse::id).containsExactly(4L);
    }

    @Test
    void missingCurrentUser_isDeniedOverview() {
        when(users.findByUsername("ben")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFeedingOverview("ben"))
                .isInstanceOf(AccessDeniedException.class).hasMessage("Current user not found");
        verifyNoInteractions(assignments, feedings);
    }

    private FeedingRequest request() {
        return new FeedingRequest("Hay", new BigDecimal("0.750"), null);
    }

    private void loadUserAndAnimal() {
        when(users.findByUsername("ben")).thenReturn(Optional.of(keeper));
        when(animals.findByIdForUpdate(5L)).thenReturn(Optional.of(animal));
    }

    private void allowAccess() {
        loadUserAndAnimal();
        when(assignments.existsActiveAssignment(2L, 7L, NOW)).thenReturn(true);
    }

    private Feedings previousFeeding(Instant time) {
        Feedings previous = Feedings.builder().id(10L).animal(animal)
                .recordedBy(User.builder().id(8L).role(Role.KEEPER).build())
                .food("Hay").amountKg(BigDecimal.ONE).fedAt(time).build();
        when(feedings.findFirstByAnimalIdOrderByFedAtDescIdDesc(5L))
                .thenReturn(Optional.of(previous));
        return previous;
    }

    private void returnSavedFeeding() {
        when(feedings.save(any(Feedings.class))).thenAnswer(invocation -> {
            Feedings feeding = invocation.getArgument(0);
            feeding.setId(20L);
            return feeding;
        });
    }

    private KeeperAssignment assignment(Long enclosureId) {
        return KeeperAssignment.builder().enclosure(Enclosure.builder().id(enclosureId).build()).build();
    }

    private Animal namedAnimal(Long id, String name) {
        return Animal.builder().id(id).name(name)
                .species(Species.builder().name("Giraffe").build())
                .enclosure(Enclosure.builder().id(2L).name("Savanna 2").build())
                .build();
    }

    private Feedings overviewFeeding(Long id, Animal animal, Instant fedAt) {
        return Feedings.builder().id(id).animal(animal)
                .recordedBy(User.builder().id(8L).fullName("Ann Lee").build())
                .food("Hay").amountKg(BigDecimal.ONE).fedAt(fedAt).note("Ate all the hay").build();
    }
}