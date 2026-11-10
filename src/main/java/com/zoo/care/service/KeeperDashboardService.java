package com.zoo.care.service;

import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperEnclosureView;
import com.zoo.care.entity.Animal;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.entity.User;
import com.zoo.care.exception.ResourceNotFoundException;
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
public class KeeperDashboardService {

    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;
    private final AnimalRepository animalRepository;


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

