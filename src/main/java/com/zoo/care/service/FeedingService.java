package com.zoo.care.service;


import com.zoo.care.dto.AnimalFeedingGroup;
import com.zoo.care.dto.FeedingHistoryResponse;
import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.FeedingResponse;
import com.zoo.care.entity.*;
import com.zoo.care.exception.AnimalDeletedException;
import com.zoo.care.exception.FeedingTooEarlyException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.FeedingRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class FeedingService {

    private final AnimalRepository animalRepository;
    private final FeedingRepository feedingRepository;
    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;
    private static final int HISTORY_DAYS = 7;

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Current user not found"));
    }

    private Animal getAnimalForFeeding(Long animalId) {
        return animalRepository.findByIdForUpdate(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal", animalId));
    }

    private void checkFeedingAccess(User user, Animal animal, Instant now) {
        if (user.getRole() != Role.KEEPER
                || !keeperAssignmentRepository.existsActiveAssignment(
                animal.getEnclosure().getId(),
                user.getId(), now)) {
            throw new AccessDeniedException(
                    "Only a keeper assigned to this enclosure can record a feeding");
        }
    }

    private void checkFeedingInterval(Animal animal, Instant now) {
        feedingRepository.findFirstByAnimalIdOrderByFedAtDescIdDesc(animal.getId())
                .ifPresent(lastFeeding -> {
                    Instant nextAllowedAt = lastFeeding.getFedAt()
                            .plus(animal.getFeedingIntervalHours(), ChronoUnit.HOURS);

                    if (now.isBefore(nextAllowedAt)) {
                        throw new FeedingTooEarlyException(nextAllowedAt);
                    }
                });
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'KEEPER')")
    @Transactional(readOnly = true)
    public List<AnimalFeedingGroup> getFeedingOverview(String username) {
        User user = getUser(username);
        Instant now = Instant.now(clock);
        Instant from = now.minus(HISTORY_DAYS, ChronoUnit.DAYS);

        List<Feedings> feedings;
        if (user.getRole() == Role.ADMIN) {
            feedings = feedingRepository.findByFedAtGreaterThanEqualOrderByFedAtDescIdDesc(from);
        } else {
            List<Long> enclosureIds = keeperAssignmentRepository.findActiveByKeeper(user.getId(), now).stream()
                    .map(assignment -> assignment.getEnclosure().getId())
                    .toList();
            if (enclosureIds.isEmpty()) {
                return List.of();
            }
            feedings = feedingRepository.findByAnimalEnclosureIdInAndFedAtGreaterThanEqualOrderByFedAtDescIdDesc(
                    enclosureIds, from);
        }

        Map<Long, List<FeedingHistoryResponse>> byAnimal = feedings.stream()
                .collect(Collectors.groupingBy(
                        feeding -> feeding.getAnimal().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(FeedingHistoryResponse::from, Collectors.toList())));

        return byAnimal.values().stream()
                .map(list -> new AnimalFeedingGroup(list.get(0), list.subList(1, list.size())))
                .toList();
    }

    @PreAuthorize("hasRole('KEEPER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FeedingResponse recordFeeding(
            Long animalId,
            FeedingRequest request,
            String username) {

        User user = getUser(username);
        Animal animal = getAnimalForFeeding(animalId);

        Instant now = Instant.now(clock);

        checkFeedingAccess(user, animal, now);

        if (animal.getDeletedStatus() == DeletedStatus.DELETED) {
            throw new AnimalDeletedException();
        }

        checkFeedingInterval(animal, now);

        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }

        Feedings saved = feedingRepository.save(
                Feedings.builder()
                        .animal(animal)
                        .recordedBy(user)
                        .food(request.food().trim())
                        .amountKg(request.amountKg())
                        .fedAt(now)
                        .note(note)
                        .build());

        log.info("Recorded feeding id={} for animal id={} by user id={}",
                saved.getId(), animal.getId(), user.getId());

        return FeedingResponse.from(saved);
    }
}
