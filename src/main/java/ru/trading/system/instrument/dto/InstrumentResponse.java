package ru.trading.system.instrument.dto;

import java.math.BigDecimal;

public record InstrumentResponse(
        String ticker,
        String name,
        int lotSize,
        BigDecimal minStep,
        String currency
) {
}
