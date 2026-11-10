package com.zoo.care.service;

import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.dto.DueFeedingItem;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperEnclosureView;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.entity.User;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.FeedingRepository;
import com.zoo.care.repository.LastFeedingTime;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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

    @PreAuthorize("hasRole('KEEPER')")
    public List<DueFeedingItem> findDueForKeeper(String username) {
        User keeper = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Current user not found"));
        if (keeper.getRole() != Role.KEEPER) {
            throw new AccessDeniedException("Only keepers can view animals due for feeding");
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

        Map<Long, Instant> lastFeedingTimes = feedingRepository.findLastFeedingTimes(
                        animals.stream().map(Animal::getId).toList()).stream()
                .collect(Collectors.toMap(LastFeedingTime::getAnimalId, LastFeedingTime::getLastFedAt));

        return animals.stream().map(animal -> {
                    Instant lastFedAt = lastFeedingTimes.get(animal.getId());
                    Instant nextAllowedAt = lastFedAt == null ? null
                            : lastFedAt.plus(animal.getFeedingIntervalHours(), ChronoUnit.HOURS);
                    return new DueFeedingItem(animal.getId(), animal.getName(),
                            animal.getSpecies().getName(), animal.getEnclosure().getName(),
                            animal.getFeedingIntervalHours(), lastFedAt, nextAllowedAt);
                })
                .filter(item -> item.nextAllowedAt() == null || !now.isBefore(item.nextAllowedAt()))
                .sorted(Comparator.comparing(DueFeedingItem::nextAllowedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(DueFeedingItem::animalName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(DueFeedingItem::animalId))
                .toList();
    }


    public List<KeeperEnclosureView> findForKeeper(String username) {
        User keeper = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Keeper '" + username + "' not found"));

        List<KeeperAssignment> assignments = keeperAssignmentRepository.findActiveByKeeper(keeper.getId(), Instant.now(clock));
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<Long> enclosureIds = assignments.stream()
                .map(assignment -> assignment.getEnclosure().getId())
                .toList();
        List<Animal> animals = animalRepository.findByEnclosureIdInOrderByNameAsc(enclosureIds);

        Map<Long, List<AnimalResponse>> animalsByEnclosure = animals.stream()
                .collect(Collectors.groupingBy(
                        animal -> animal.getEnclosure().getId(),
                        Collectors.mapping(AnimalResponse::from, Collectors.toList())));

        return assignments.stream()
                .map(assignment -> new KeeperEnclosureView(
                        EnclosureResponse.from(assignment.getEnclosure()),
                        animalsByEnclosure.getOrDefault(assignment.getEnclosure().getId(), List.of())))
                .toList();
    }
}

