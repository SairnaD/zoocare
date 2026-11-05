package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.dto.KeeperResponse;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
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

import com.zoo.care.entity.User;
import com.zoo.care.entity.Role;


import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnclosureService {

    private final UserRepository userRepository;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;

    private final EnclosureRepository enclosureRepository;
    private final AnimalRepository animalRepository;

    private KeeperAssignment getAssignment(Long enclosureId, Long assignmentId) {
        KeeperAssignment assignment = keeperAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("KeeperAssignment", assignmentId));

        if (!assignment.getEnclosure().getId().equals(enclosureId)) {
            throw new ResourceNotFoundException("KeeperAssignment", assignmentId);
        }

        return assignment;
    }


    private void validateActiveAssignment(KeeperAssignment assignment, Instant now) {
        if (assignment.getStartsAt().isAfter(now)
                || (assignment.getEndsAt() != null
                && !assignment.getEndsAt().isAfter(now))) {
            throw new BusinessRuleException("Keeper assignment is not active");
        }
    }

    @Transactional
    public KeeperAssignmentResponse switchKeeper(
            Long enclosureId,
            Long assignmentId,
            Long newKeeperId) {

        Enclosure enclosure = getEnclosure(enclosureId);
        KeeperAssignment oldAssignment = getAssignment(enclosureId, assignmentId);

        Instant now = Instant.now(clock);
        validateActiveAssignment(oldAssignment, now);

        User newKeeper = getKeeper(newKeeperId);

        if (oldAssignment.getKeeper().getId().equals(newKeeperId)) {
            throw new BusinessRuleException(
                    "New keeper must be different from the current keeper");
        }

        if (keeperAssignmentRepository.existsActiveAssignment(
                enclosureId, newKeeperId, now)) {
            throw new BusinessRuleException(
                    "Keeper is already assigned to this enclosure");
        }

        oldAssignment.setEndsAt(now);

        KeeperAssignment saved = keeperAssignmentRepository.save(
                KeeperAssignment.builder()
                        .enclosure(enclosure)
                        .keeper(newKeeper)
                        .startsAt(now)
                        .endsAt(null)
                        .createdAt(now)
                        .build());

        return KeeperAssignmentResponse.from(saved);
    }



    public List<EnclosureResponse> findAll() {
        return enclosureRepository.findAll(Sort.by("name")).stream()
                .map(EnclosureResponse::from)
                .toList();
    }

    public EnclosureResponse findById(Long id) {
        return EnclosureResponse.from(getEnclosure(id));
    }

    @Transactional
    public EnclosureResponse create(EnclosureRequest request) {
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Enclosure enclosure = Enclosure.builder()
                .name(name)
                .habitatType(request.habitatType())
                .capacity(request.capacity())
                .build();
        Enclosure saved = enclosureRepository.save(enclosure);
        log.info("Created enclosure '{}' (id={})", saved.getName(), saved.getId());
        return EnclosureResponse.from(saved);
    }

    @Transactional
    public EnclosureResponse update(Long id, EnclosureRequest request) {
        Enclosure enclosure = getEnclosure(id);
        String name = request.name().trim();
        if (enclosureRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        if (enclosure.getHabitatType() != request.habitatType()
                && animalRepository.existsByEnclosureId(id)) {
            throw new BusinessRuleException("Habitat cannot be changed while the enclosure has animals");
        }
        if (animalRepository.countByEnclosureId(id) > request.capacity()) {
            throw new BusinessRuleException("Capacity cannot be lower than the number of animals in the enclosure");
        }
        enclosure.setName(name);
        enclosure.setHabitatType(request.habitatType());
        enclosure.setCapacity(request.capacity());
        log.info("Updated enclosure '{}' (id={})", name, id);
        return EnclosureResponse.from(enclosure);
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
            throw new BusinessRuleException("Enclosure still has animals and cannot be deleted");
        }
        log.info("Deleted enclosure '{}' (id={})", enclosure.getName(), id);
    }

    private Enclosure getEnclosure(Long id) {
        return enclosureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enclosure", id));
    }


    private User getKeeper(Long keeperId) {
        User keeper = userRepository.findById(keeperId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", keeperId));

        if (keeper.getRole() != Role.KEEPER) {
            throw new BusinessRuleException(
                    "Only users with role KEEPER can be assigned to an enclosure"
            );
        }

        return keeper;
    }

    @Transactional
    public KeeperAssignmentResponse assignKeeper(Long enclosureId, Long keeperId) {
        Enclosure enclosure = getEnclosure(enclosureId);
        User keeper = getKeeper(keeperId);
        Instant now = Instant.now(clock);

        if (keeperAssignmentRepository.existsActiveAssignment(enclosureId, keeperId, now)) {
            throw new BusinessRuleException("Keeper is already assigned to this enclosure");
        }

        KeeperAssignment saved = keeperAssignmentRepository.save(KeeperAssignment.builder()
                .enclosure(enclosure)
                .keeper(keeper)
                .startsAt(now)
                .endsAt(null)
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Keeper " + keeperId + " is not assigned to enclosure " + enclosureId));
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
        return userRepository.findAllByRoleOrderByFullNameAsc(Role.KEEPER).stream()
                .map(KeeperResponse::from)
                .toList();
    }


    private BusinessRuleException duplicateName(String name) {
        return new BusinessRuleException("Enclosure with name '" + name + "' already exists");
    }
}