package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.dto.KeeperResponse;
import com.zoo.care.entity.*;
import com.zoo.care.exception.*;
import com.zoo.care.repository.AnimalRepository;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnclosureService {

    private final EnclosureRepository enclosureRepository;
    private final AnimalRepository animalRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public List<EnclosureResponse> findAll() {
        Map<Long, List<String>> keeperNamesByEnclosure = keeperAssignmentRepository
                .findAllActive(Instant.now(clock)).stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getEnclosure().getId(),
                        Collectors.mapping(assignment -> assignment.getKeeper().getFullName(), Collectors.toList())));
        return enclosureRepository.findAll(Sort.by("name")).stream()
                .map(enclosure -> EnclosureResponse.from(enclosure,
                        keeperNamesByEnclosure.getOrDefault(enclosure.getId(), List.of())))
                .toList();
    }

    public EnclosureResponse findById(Long id) {
        return toResponseWithKeepers(getEnclosure(id));
    }

    @Transactional
    public EnclosureResponse create(EnclosureRequest request) {
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCase(name)) {
            throw new EnclosureAlreadyExistsException(name);
        }
        Enclosure saved = enclosureRepository.save(Enclosure.builder()
                .name(name)
                .habitatType(request.habitatType())
                .capacity(request.capacity())
                .build());
        log.info("Created enclosure '{}' (id={})", saved.getName(), saved.getId());
        return EnclosureResponse.from(saved);
    }

    @Transactional
    public EnclosureResponse update(Long id, EnclosureRequest request) {
        Enclosure enclosure = getEnclosure(id);
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new EnclosureAlreadyExistsException(name);
        }
        long animalCount = animalRepository.countByEnclosureId(id);
        if (animalCount > 0 && enclosure.getHabitatType() != request.habitatType()) {
            throw new EnclosureHabitatLockedException();
        }
        if (animalCount > request.capacity()) {
            throw new EnclosureCapacityTooLowException(animalCount);
        }
        enclosure.setName(name);
        enclosure.setHabitatType(request.habitatType());
        enclosure.setCapacity(request.capacity());
        log.info("Updated enclosure '{}' (id={})", name, id);
        return toResponseWithKeepers(enclosure);
    }

    @Transactional
    public void delete(Long id) {
        Enclosure enclosure = getEnclosure(id);
        keeperAssignmentRepository.deleteByEnclosureId(id);
        try {
            enclosureRepository.delete(enclosure);
            enclosureRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.info("Enclosure id={} cannot be deleted: it still has animals", id);
            throw new EnclosureNotEmptyException();
        }
        log.info("Deleted enclosure '{}' (id={})", enclosure.getName(), id);
    }

    @Transactional
    public KeeperAssignmentResponse assignKeeper(Long enclosureId, Long keeperId) {
        Enclosure enclosure = getEnclosure(enclosureId);
        User keeper = getKeeper(keeperId);
        Instant now = Instant.now(clock);
        if (keeperAssignmentRepository.existsActiveAssignment(enclosureId, keeperId, now)) {
            throw new KeeperAlreadyAssignedException();
        }
        KeeperAssignment saved = keeperAssignmentRepository.save(KeeperAssignment.builder()
                .enclosure(enclosure)
                .keeper(keeper)
                .startsAt(now)
                .createdAt(now)
                .build());
        log.info("Created keeper assignment id={} for enclosure id={} and keeper id={}",
                saved.getId(), enclosureId, keeperId);
        return KeeperAssignmentResponse.from(saved);
    }

    @Transactional
    public void unassignKeeper(Long enclosureId, Long keeperId) {
        getEnclosure(enclosureId);
        Instant now = Instant.now(clock);
        KeeperAssignment assignment = keeperAssignmentRepository.findActive(enclosureId, keeperId, now)
                .orElseThrow(() -> new KeeperAssignmentNotFoundException(enclosureId, keeperId));
        assignment.setEndsAt(now);
        log.info("Ended keeper assignment id={} (enclosure id={}, keeper id={})",
                assignment.getId(), enclosureId, keeperId);
    }

    public List<KeeperAssignmentResponse> findActiveKeepers(Long enclosureId) {
        getEnclosure(enclosureId);
        return keeperAssignmentRepository.findActiveByEnclosure(enclosureId, Instant.now(clock)).stream()
                .map(KeeperAssignmentResponse::from)
                .toList();
    }

    public List<KeeperResponse> findAllKeepers() {
        return userRepository.findAllByRoleAndDeletedStatusOrderByFullName(Role.KEEPER, DeletedStatus.PRESENT).stream()
                .map(KeeperResponse::from)
                .toList();
    }

    private Enclosure getEnclosure(Long id) {
        return enclosureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", id));
    }

    private User getKeeper(Long keeperId) {
        User keeper = userRepository.findById(keeperId)
                .filter(user -> user.getDeletedStatus() == DeletedStatus.PRESENT)
                .orElseThrow(() -> new ResourceNotFoundException("User", keeperId));
        if (keeper.getRole() != Role.KEEPER) {
            throw new UserIsNotKeeperException();
        }
        return keeper;
    }

    private EnclosureResponse toResponseWithKeepers(Enclosure enclosure) {
        List<String> keeperNames = keeperAssignmentRepository
                .findActiveByEnclosure(enclosure.getId(), Instant.now(clock)).stream()
                .map(assignment -> assignment.getKeeper().getFullName())
                .toList();
        return EnclosureResponse.from(enclosure, keeperNames);
    }
}