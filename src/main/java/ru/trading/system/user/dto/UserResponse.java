package ru.trading.system.user.dto;

import ru.trading.system.user.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Role role,
        OffsetDateTime createdAt
) {
}
