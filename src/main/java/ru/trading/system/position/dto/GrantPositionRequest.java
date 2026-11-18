package ru.trading.system.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record GrantPositionRequest(
    @NotNull UUID accountId, @NotBlank String ticker, @Positive long quantity) {}
