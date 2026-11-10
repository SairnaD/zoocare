package com.zoo.care.service;

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
}
