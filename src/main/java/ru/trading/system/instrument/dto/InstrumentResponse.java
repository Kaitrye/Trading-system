package ru.trading.system.instrument.dto;

import ru.trading.system.instrument.InstrumentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstrumentResponse(
        String ticker,
        String name,
        int lotSize,
        BigDecimal minStep,
        String currency,
        InstrumentType type,
        BigDecimal nominal,
        LocalDate maturityDate,
        BigDecimal couponRate,
        Integer couponFrequencyMonths,
        LocalDate nextCouponDate
) {
}
