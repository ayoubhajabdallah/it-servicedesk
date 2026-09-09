package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import dev.ayoub.servicedesk.dto.AuthResponse;
import dev.ayoub.servicedesk.dto.LoginRequest;
import dev.ayoub.servicedesk.dto.RegisterRequest;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import dev.ayoub.servicedesk.dto.AuthResponse;
import dev.ayoub.servicedesk.dto.LoginRequest;

@Service
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.EMPLOYEE)
                .build();

        return userRepository.save(user);
    }
    public AuthResponse login(LoginRequest request) {

    User user = userRepository
            .findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

    if (!passwordEncoder.matches(
            request.password(),
            user.getPassword())) {

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
        );
    }

    String token = jwtService.generateToken(user);

    return new AuthResponse(
            token,
            "Bearer",
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().name()
    );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
