package ru.trading.system.user.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
