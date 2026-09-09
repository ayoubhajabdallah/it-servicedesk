package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import dev.ayoub.servicedesk.dto.RegisterRequest;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateEmployeeWithHashedPassword() {

        RegisterRequest request =
                new RegisterRequest(
                        "Ayoub",
                        "  AYOUB@Example.COM  ",
                        "StrongPass123!"
                );

        when(userRepository.existsByEmail("ayoub@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPass123!"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        User result = authService.register(request);

        assertEquals(1L, result.getId());
        assertEquals("Ayoub", result.getName());
        assertEquals("ayoub@example.com", result.getEmail());
        assertEquals("hashed-password", result.getPassword());
        assertEquals(UserRole.EMPLOYEE, result.getRole());

        verify(passwordEncoder).encode("StrongPass123!");
        verify(userRepository).existsByEmail("ayoub@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {

        RegisterRequest request =
                new RegisterRequest(
                        "Ayoub",
                        "  AYOUB@Example.COM  ",
                        "StrongPass123!"
                );

        when(userRepository.existsByEmail("ayoub@example.com"))
                .thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> authService.register(request)
                );

        assertEquals(
                "Email already registered",
                exception.getReason()
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository, never()).save(any());
    }
}
