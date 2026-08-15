package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import dev.ayoub.servicedesk.dto.AuthResponse;
import dev.ayoub.servicedesk.dto.LoginRequest;
import dev.ayoub.servicedesk.dto.RegisterRequest;
import dev.ayoub.servicedesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.EMPLOYEE)
                .build();

        return userRepository.save(user);
    }
    public AuthResponse login(LoginRequest request) {

    User user = userRepository
            .findByEmail(request.email().toLowerCase())
            .orElseThrow(() ->
                    new IllegalArgumentException("Invalid email or password"));

    if (!passwordEncoder.matches(
            request.password(),
            user.getPassword())) {

        throw new IllegalArgumentException(
                "Invalid email or password"
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
}