package com.jobboard.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;
    private final String seedName;

    public AdminSeeder(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${app.admin.seed-email}") String seedEmail,
                        @Value("${app.admin.seed-password}") String seedPassword,
                        @Value("${app.admin.seed-name}") String seedName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
        this.seedName = seedName;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(seedEmail)) {
            return;
        }
        User admin = new User(seedEmail, passwordEncoder.encode(seedPassword), seedName, UserRole.ADMIN);
        userRepository.save(admin);
    }
}
