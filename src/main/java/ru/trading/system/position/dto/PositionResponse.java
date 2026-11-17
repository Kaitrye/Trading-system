package ru.trading.system.position.dto;

import java.util.UUID;

public record PositionResponse(
        UUID accountId,
        String ticker,
        String instrumentName,
        long quantity,
        long reservedQuantity,
        long availableQuantity
) {
}
