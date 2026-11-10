package com.zoo.care.service;


import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.FeedingResponse;
import com.zoo.care.exception.AnimalDeletedException;
import com.zoo.care.exception.FeedingTooEarlyException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.FeedingRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Clock;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.User;
import com.zoo.care.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.zoo.care.entity.Role;
import java.time.Instant;
import com.zoo.care.exception.BusinessRuleException;
import java.time.temporal.ChronoUnit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Feedings;


@Service
@RequiredArgsConstructor
@Slf4j
public class FeedingService {

    private final AnimalRepository animalRepository;
    private final FeedingRepository feedingRepository;
    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;

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
