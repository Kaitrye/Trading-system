package ru.trading.system.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import ru.trading.system.user.Role;

public record UserResponse(UUID id, String email, Role role, OffsetDateTime createdAt) {}
