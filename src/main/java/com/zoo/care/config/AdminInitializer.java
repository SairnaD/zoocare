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

    // Credentials come from application properties (app.admin.*).
    // The values after ':' are defaults for local development only.
    // TODO: before deploying, set app.admin.username and app.admin.password
    //  so the default admin123 password is never used in production.
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email("admin@test.com")
                    .fullName("Admin")
                    .role(Role.ADMIN)
                    .build();
            User savedAdmin = userRepository.save(admin);
            log.info("Registered new admin ('{}')", savedAdmin.getUsername());
        }
    }
}
