package com.example.library.config;

import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.repository.RoleRepository;
import com.example.library.repository.UserRepository;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createDefaultUsers(
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));
            Role userRole = roleRepository.findByName("USER").orElseGet(() -> roleRepository.save(new Role("USER")));

            if (!userRepository.existsByEmail("admin@library.test")) {
                User admin = new User();
                admin.setFullName("Library Admin");
                admin.setEmail("admin@library.test");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRoles(Set.of(adminRole, userRole));
                userRepository.save(admin);
            }

            if (!userRepository.existsByEmail("user@library.test")) {
                User user = new User();
                user.setFullName("Demo User");
                user.setEmail("user@library.test");
                user.setPassword(passwordEncoder.encode("user12345"));
                user.setRoles(Set.of(userRole));
                userRepository.save(user);
            }
        };
    }
}
