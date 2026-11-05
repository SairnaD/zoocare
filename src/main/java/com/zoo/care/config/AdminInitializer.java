package com.zoo.care.config;

import com.zoo.care.entity.Role;
import com.zoo.care.entity.User;
import com.zoo.care.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the default admin account on application startup.
 * Registration only creates keepers, so the first admin has to be created here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email("admin@zoocare.local")
                    .fullName("Admin")
                    .role(Role.ADMIN)
                    .build();
            User savedAdmin = userRepository.save(admin);
            log.info("Registered new admin ('{}')", savedAdmin.getUsername());
        }
    }
}
