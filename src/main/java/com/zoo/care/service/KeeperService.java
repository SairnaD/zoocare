package com.zoo.care.service;

import com.zoo.care.dto.EnclosureAnimalCount;
import com.zoo.care.dto.EnclosureSummary;
import com.zoo.care.dto.KeeperOverviewResponse;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Role;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KeeperService {

    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final AnimalRepository animalRepository;
    private final Clock clock;

    public List<KeeperOverviewResponse> findAllWithOverview() {
        Map<Long, List<EnclosureSummary>> enclosuresByKeeper = keeperAssignmentRepository
                .findAllActive(Instant.now(clock)).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getKeeper().getId(),
                        Collectors.mapping(assignment -> EnclosureSummary.from(assignment.getEnclosure()),
                                Collectors.toList())));
        Map<Long, Long> animalsByEnclosure = animalRepository
                .countAnimalsPerEnclosure(DeletedStatus.PRESENT).stream()
                .collect(Collectors.toMap(EnclosureAnimalCount::enclosureId, EnclosureAnimalCount::animalCount));

        return userRepository.findAllByRoleAndDeletedStatusOrderByFullName(Role.KEEPER, DeletedStatus.PRESENT).stream()
                .map(keeper -> {
                    List<EnclosureSummary> enclosures = enclosuresByKeeper.getOrDefault(keeper.getId(), List.of());
                    long animalCount = enclosures.stream()
                            .mapToLong(enclosure -> animalsByEnclosure.getOrDefault(enclosure.id(), 0L))
                            .sum();
                    return KeeperOverviewResponse.from(keeper, enclosures, animalCount);
                })
                .toList();
    }
}