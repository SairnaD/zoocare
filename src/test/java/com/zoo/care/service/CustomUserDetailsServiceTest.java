package com.zoo.care.service;

import com.zoo.care.entity.DeletedStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void loadUserByUsername_keeperReturnsDetailsWithKeeperAuthority() {
        when(userRepository.findByUsername("Ben")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("Ben");

        assertEquals("Ben", details.getUsername());
        assertEquals("hashedPassword", details.getPassword());
        assertEquals(1, details.getAuthorities().size());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_KEEPER")));
        assertTrue(details.isEnabled());
    }

    @Test
    void loadUserByUsername_adminReturnsAdminAuthority() {
        User admin = User.builder()
                .username("admin")
                .password("adminHash")
                .email("admin@zoocare.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails details = customUserDetailsService.loadUserByUsername("admin");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_unknownUserThrowsException() {
        when(userRepository.findByUsername("Ben")).thenReturn(Optional.empty());

        UsernameNotFoundException e = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("Ben"));

        assertEquals("User not found: Ben", e.getMessage());
    }

    @Test
    void loadUserByUsername_deletedUserIsDisabled() {
        user.setDeletedStatus(DeletedStatus.DELETED);
        when(userRepository.findByUsername("Ben")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("Ben");

        assertFalse(details.isEnabled());
    }
}