package ru.trading.system.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.trading.system.order.OrderSide;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String ticker,
        @NotNull OrderSide side,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal price,
        @Positive long quantity
) {
}
