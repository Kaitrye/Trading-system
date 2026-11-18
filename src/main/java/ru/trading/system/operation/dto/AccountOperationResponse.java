package ru.trading.system.operation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.trading.system.operation.OperationType;

public record AccountOperationResponse(
    UUID id,
    UUID accountId,
    UUID tradeId,
    OperationType type,
    BigDecimal amount,
    String currency,
    OffsetDateTime createdAt) {}
