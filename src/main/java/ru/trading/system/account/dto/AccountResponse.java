package ru.trading.system.account.dto;

import java.math.BigDecimal;
import java.util.UUID;
import ru.trading.system.account.AccountStatus;

public record AccountResponse(
    UUID id,
    String currency,
    BigDecimal balance,
    BigDecimal reservedBalance,
    BigDecimal availableBalance,
    AccountStatus status) {}
