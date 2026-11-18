package ru.trading.system.trade.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.trading.system.order.OrderSide;

public record MyTradeResponse(
    UUID id,
    UUID accountId,
    String ticker,
    OrderSide side,
    BigDecimal price,
    long quantity,
    OffsetDateTime executedAt) {}
