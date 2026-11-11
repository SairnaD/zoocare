package com.zoo.care.service;

import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.entity.DeletedStatus;
import com.zoo.care.entity.KeeperAssignment;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.exception.EmailAlreadyRegisteredException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.exception.UserIsNotKeeperException;
import com.zoo.care.exception.UsernameAlreadyTakenException;
import com.zoo.care.repository.KeeperAssignmentRepository;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KeeperAssignmentRepository keeperAssignmentRepository;

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @InjectMocks
    private UserService userService;

    private final RegisterRequest request =
            new RegisterRequest("Ben", "password123", "ben@test.com", "Ben Ten");

    @Test
    void register_takenUsernameThrowsException() {
        when(userRepository.existsByUsername("Ben")).thenReturn(true);
        UsernameAlreadyTakenException e = assertThrows(UsernameAlreadyTakenException.class,
                () -> userService.register(request));

        assertEquals("Username already taken", e.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_takenEmailThrowsException() {
        when(userRepository.existsByUsername("Ben")).thenReturn(false);
        when(userRepository.existsByEmail("ben@test.com")).thenReturn(true);

        EmailAlreadyRegisteredException e = assertThrows(EmailAlreadyRegisteredException.class,
                () -> userService.register(request));

        assertEquals("Email already registered", e.getMessage());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_validDataSavesKeeperWithHashedPassword() {
        when(userRepository.existsByUsername("Ben")).thenReturn(false);
        when(userRepository.existsByEmail("ben@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("Ben", saved.getUsername());
        assertEquals("hashedPassword", saved.getPassword());
        assertEquals("ben@test.com", saved.getEmail());
        assertEquals("Ben Ten", saved.getFullName());
        assertEquals(Role.KEEPER, saved.getRole());
    }

    @Test
    void deleteKeeper_marksDeletedAndEndsActiveAssignments() {
        User keeper = keeper();
        KeeperAssignment assignment = KeeperAssignment.builder().keeper(keeper).build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(keeper));
        when(keeperAssignmentRepository.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment));

        userService.deleteKeeper(7L);

        assertEquals(DeletedStatus.DELETED, keeper.getDeletedStatus());
        assertEquals(Role.KEEPER, keeper.getRole());
        assertEquals(NOW, assignment.getEndsAt());
    }

    @Test
    void makeAdmin_changesRoleAndEndsActiveAssignments() {
        User keeper = keeper();
        KeeperAssignment assignment = KeeperAssignment.builder().keeper(keeper).build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(keeper));
        when(keeperAssignmentRepository.findActiveByKeeper(7L, NOW)).thenReturn(List.of(assignment));

        userService.makeAdmin(7L);

        assertEquals(Role.ADMIN, keeper.getRole());
        assertEquals(DeletedStatus.PRESENT, keeper.getDeletedStatus());
        assertEquals(NOW, assignment.getEndsAt());
    }

    @Test
    void deleteKeeper_adminIsRejected() {
        User admin = keeper();
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThrows(UserIsNotKeeperException.class, () -> userService.deleteKeeper(7L));
        assertEquals(DeletedStatus.PRESENT, admin.getDeletedStatus());
        verifyNoInteractions(keeperAssignmentRepository);
    }

    @Test
    void makeAdmin_adminIsRejected() {
        User admin = keeper();
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThrows(UserIsNotKeeperException.class, () -> userService.makeAdmin(7L));
        verifyNoInteractions(keeperAssignmentRepository);
    }

    @Test
    void deletedKeeper_isNotFoundForDeleteOrPromotion() {
        User deleted = keeper();
        deleted.setDeletedStatus(DeletedStatus.DELETED);
        when(userRepository.findById(7L)).thenReturn(Optional.of(deleted));

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteKeeper(7L));
        assertThrows(ResourceNotFoundException.class, () -> userService.makeAdmin(7L));
        assertEquals(Role.KEEPER, deleted.getRole());
        verifyNoInteractions(keeperAssignmentRepository);
    }

    @Test
    void unknownUser_isNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteKeeper(99L));

        assertEquals("User with id 99 not found", e.getMessage());
        verifyNoInteractions(keeperAssignmentRepository);
    }

    private User keeper() {
        return User.builder().id(7L).username("ben").fullName("Ben Ten").role(Role.KEEPER).build();
    }
}