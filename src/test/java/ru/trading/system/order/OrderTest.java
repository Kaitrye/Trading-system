package ru.trading.system.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {

    @Test
    void changesStatusWhenFilledPartiallyAndCompletely() {
        Order order = new Order(null, null, OrderSide.BUY, new BigDecimal("100"), 20);

        order.fill(5);

        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertEquals(15, order.getRemainingQuantity());

        order.fill(15);

        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertEquals(0, order.getRemainingQuantity());
    }
}
