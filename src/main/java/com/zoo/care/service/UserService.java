package com.zoo.care.service;

import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.exception.EmailAlreadyRegisteredException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.exception.UserIsNotKeeperException;
import com.zoo.care.exception.UsernameAlreadyTakenException;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeeperAssignmentRepository keeperAssignmentRepository;
    private final Clock clock;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException();
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .fullName(request.fullName())
                .role(Role.KEEPER)
                .build();
        User saved = userRepository.save(user);
        log.info("Registered new user '{}' with role {}", saved.getUsername(), saved.getRole());
        return saved;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteKeeper(Long keeperId) {
        User keeper = getActiveKeeper(keeperId);
        keeper.setDeletedStatus(DeletedStatus.DELETED);
        endActiveAssignments(keeper);
        log.info("Soft deleted keeper {}", keeper.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void makeAdmin(Long keeperId) {
        User keeper = getActiveKeeper(keeperId);
        keeper.setRole(Role.ADMIN);
        endActiveAssignments(keeper);
        log.info("Promoted keeper {} to admin", keeper.getId());
    }

    private User getActiveKeeper(Long keeperId) {
        User keeper = userRepository.findById(keeperId)
                .filter(user -> user.getDeletedStatus() == DeletedStatus.PRESENT)
                .orElseThrow(() -> new ResourceNotFoundException("User", keeperId));
        if (keeper.getRole() != Role.KEEPER) {
            throw new UserIsNotKeeperException();
        }
        return keeper;
    }

    private void endActiveAssignments(User keeper) {
        Instant now = Instant.now(clock);
        keeperAssignmentRepository.findActiveByKeeper(keeper.getId(), now)
                .forEach(assignment -> assignment.setEndsAt(now));
    }
}
