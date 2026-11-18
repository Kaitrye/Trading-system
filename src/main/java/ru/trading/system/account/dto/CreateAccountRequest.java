package ru.trading.system.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(@NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {}
