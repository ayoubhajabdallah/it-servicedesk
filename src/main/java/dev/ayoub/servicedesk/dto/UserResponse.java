package dev.ayoub.servicedesk.dto;

import dev.ayoub.servicedesk.domain.UserRole;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
}