package ru.trading.system.trade.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TradeResponse(
    UUID id, String ticker, BigDecimal price, long quantity, OffsetDateTime executedAt) {}
