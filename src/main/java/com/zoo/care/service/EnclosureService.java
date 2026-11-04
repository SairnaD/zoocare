package com.zoo.care.service;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        enclosure.setName(name);
        enclosure.setHabitatType(request.habitatType());
        enclosure.setCapacity(request.capacity());
        log.info("Updated enclosure '{}' (id={})", name, id);
        return EnclosureResponse.from(enclosure);
    }

    @Transactional
    public void delete(Long id) {
        Enclosure enclosure = getEnclosure(id);
        enclosureRepository.delete(enclosure);
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
        if (enclosureId == null || enclosureId <= 0) {
            throw new BusinessRuleException(
                    "Enclosure ID must be positive"
            );
        }

        if (keeperId == null || keeperId <= 0) {
            throw new BusinessRuleException(
                    "Keeper ID must be positive"
            );
        }

        Enclosure enclosure = getEnclosure(enclosureId);
        User keeper = getKeeper(keeperId);

        Instant now = Instant.now(clock);

        boolean alreadyAssigned =
                keeperAssignmentRepository.existsActiveAssignment(
                        enclosureId,
                        keeperId,
                        now
                );

        if (alreadyAssigned) {
            throw new BusinessRuleException(
                    "Keeper is already assigned to this enclosure"
            );
        }

        KeeperAssignment assignment = KeeperAssignment.builder()
                .enclosure(enclosure)
                .keeper(keeper)
                .startsAt(now)
                .endsAt(null)
                .createdAt(now)
                .build();

        KeeperAssignment saved =
                keeperAssignmentRepository.save(assignment);

        log.info(
                "Created keeper assignment id={} for enclosure id={} and keeper id={}",
                saved.getId(),
                enclosureId,
                keeperId
        );

        return KeeperAssignmentResponse.from(saved);
    }


    private BusinessRuleException duplicateName(String name) {
        return new BusinessRuleException("Enclosure with name '" + name + "' already exists");
    }
}