package com.zoo.care.service;

import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessRuleException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Email already registered");
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
}
