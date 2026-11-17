package ru.trading.system.operation.dto;

import ru.trading.system.operation.OperationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountOperationResponse(
        UUID id,
        UUID accountId,
        UUID tradeId,
        OperationType type,
        BigDecimal amount,
        String currency,
        OffsetDateTime createdAt
) {
}
