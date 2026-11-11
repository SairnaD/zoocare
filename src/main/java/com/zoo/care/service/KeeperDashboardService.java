package com.zoo.care.service;

import com.zoo.care.dto.KeeperAnimalView;
import com.zoo.care.entity.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.FeedingRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeeperDashboardService {

    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;
    private final AnimalRepository animalRepository;
    private final FeedingRepository feedingRepository;

    private static final Duration FEED_SOON_WINDOW = Duration.ofHours(1);

    @PreAuthorize("hasRole('KEEPER')")
    public List<KeeperAnimalView> findAnimalsForKeeper(String username) {
        User keeper = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Current user not found"));
        if (keeper.getRole() != Role.KEEPER) {
            throw new AccessDeniedException("Only keepers can view their animals");
        }

        Instant now = Instant.now(clock);
        List<Long> enclosureIds = keeperAssignmentRepository.findActiveByKeeper(keeper.getId(), now)
                .stream().map(assignment -> assignment.getEnclosure().getId()).distinct().toList();
        if (enclosureIds.isEmpty()) {
            return List.of();
        }

        List<Animal> animals = animalRepository.findByEnclosureIdInOrderByNameAsc(enclosureIds)
                .stream().filter(animal -> animal.getDeletedStatus() == DeletedStatus.PRESENT).toList();
        if (animals.isEmpty()) {
            return List.of();
        }

        Map<Long, Feedings> lastFeedings = feedingRepository.findLatestByAnimalIds(
                        animals.stream().map(Animal::getId).toList()).stream()
                .collect(Collectors.toMap(feeding -> feeding.getAnimal().getId(), feeding -> feeding,
                        (first, second) -> first.getId() > second.getId() ? first : second));

        LocalDate today = LocalDate.now(clock);
        return animals.stream()
                .map(animal -> toView(animal, lastFeedings.get(animal.getId()), now, today))
                .sorted(Comparator.comparing(KeeperAnimalView::status)
                        .thenComparing(KeeperAnimalView::nextFeedingAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(KeeperAnimalView::animalName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private KeeperAnimalView toView(Animal animal, Feedings lastFeeding, Instant now, LocalDate today) {
        Instant lastFedAt = lastFeeding == null ? null : lastFeeding.getFedAt();
        Instant nextFeedingAt = lastFedAt == null ? null
                : lastFedAt.plus(animal.getFeedingIntervalHours(), ChronoUnit.HOURS);
        return new KeeperAnimalView(
                animal.getId(),
                animal.getName(),
                animal.getSpecies().getName(),
                animal.getEnclosure().getName(),
                animal.getEnclosure().getHabitatType(),
                animal.getBirthDate(),
                Period.between(animal.getBirthDate(), today).getYears(),
                animal.getWeight(),
                animal.getDescription(),
                animal.getFeedingIntervalHours(),
                lastFedAt,
                lastFeeding == null ? null : lastFeeding.getRecordedBy().getFullName(),
                nextFeedingAt,
                statusOf(nextFeedingAt, now));
    }

    private FeedingStatus statusOf(Instant nextFeedingAt, Instant now) {
        if (nextFeedingAt == null || !now.isBefore(nextFeedingAt)) {
            return FeedingStatus.NEED_TO_FEED;
        }
        if (!now.plus(FEED_SOON_WINDOW).isBefore(nextFeedingAt)) {
            return FeedingStatus.FEED_SOON;
        }
        return FeedingStatus.FED;
    }
}
