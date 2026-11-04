package com.zoo.care.config;

import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin123");
    }

    @Test
    void run_existingAdminIsNotCreated() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        adminInitializer.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_missingAdminIsCreatedWithHashedPassword() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        adminInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("hashedPassword", saved.getPassword());
        assertEquals(Role.ADMIN, saved.getRole());
    }
}