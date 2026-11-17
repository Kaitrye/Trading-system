package ru.trading.system.trade.dto;

import ru.trading.system.order.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MyTradeResponse(
        UUID id,
        UUID accountId,
        String ticker,
        OrderSide side,
        BigDecimal price,
        long quantity,
        OffsetDateTime executedAt
) {
}
