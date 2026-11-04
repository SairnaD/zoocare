package com.zoo.care.service;

import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private final User user = User.builder()
            .username("Ben")
            .password("hashedPassword")
            .email("ben@test.com")
            .fullName("Ben Ten")
            .role(Role.KEEPER)
            .build();

    @Test
    void loadUserByUsername_unknownUserThrowsException() {
        when(userRepository.findByUsername("Ben")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("Ben"));
    }
}