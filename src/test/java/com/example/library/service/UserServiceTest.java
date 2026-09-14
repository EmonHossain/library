package com.example.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.library.dto.RegistrationRequest;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.repository.RoleRepository;
import com.example.library.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsUserRole() {
        RegistrationRequest request = new RegistrationRequest();
        request.setFullName("  Demo User  ");
        request.setEmail("  USER@Example.COM  ");
        request.setPassword("secret123");
        Role userRole = new Role("USER");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo("Demo User");
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRoles()).containsExactly(userRole);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("user@example.com");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("An account already exists for this email.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserDoesNotAllowAdminToDisableOwnAccount() {
        User admin = new User();
        admin.setEmail("admin@library.test");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.updateUser(1L, false, Set.of("ADMIN"), "admin@library.test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("You cannot disable your own account.");
    }
}
