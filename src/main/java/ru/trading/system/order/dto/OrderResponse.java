package ru.trading.system.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.trading.system.order.OrderSide;
import ru.trading.system.order.OrderStatus;

public record OrderResponse(
    UUID id,
    UUID accountId,
    String ticker,
    OrderSide side,
    BigDecimal price,
    long quantity,
    long remainingQuantity,
    OrderStatus status,
    OffsetDateTime createdAt) {}
