package ru.trading.system.instrument.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import ru.trading.system.instrument.InstrumentType;

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
    LocalDate nextCouponDate,
    String baseCurrency,
    String quoteCurrency) {}
