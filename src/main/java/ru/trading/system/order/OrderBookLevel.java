package ru.trading.system.order;

import java.math.BigDecimal;

public interface OrderBookLevel {
    BigDecimal getPrice();
    long getQuantity();
}
