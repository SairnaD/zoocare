package com.zoo.care.service;

import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.exception.EmailAlreadyRegisteredException;
import com.zoo.care.exception.UsernameAlreadyTakenException;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
}