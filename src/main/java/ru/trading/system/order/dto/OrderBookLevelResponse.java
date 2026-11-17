package ru.trading.system.order.dto;

import java.math.BigDecimal;

public record OrderBookLevelResponse(BigDecimal price, long quantity) {
}
