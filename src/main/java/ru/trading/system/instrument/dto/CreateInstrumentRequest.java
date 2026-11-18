package ru.trading.system.instrument.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import ru.trading.system.instrument.InstrumentType;

public record CreateInstrumentRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z]{1,16}") String ticker,
    @NotBlank @Size(max = 120) String name,
    @Positive int lotSize,
    @NotNull @DecimalMin(value = "0.0001") BigDecimal minStep,
    @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
    @NotNull InstrumentType type,
    @DecimalMin(value = "0.0001") BigDecimal nominal,
    LocalDate maturityDate,
    @DecimalMin(value = "0.0001") BigDecimal couponRate,
    @Positive Integer couponFrequencyMonths,
    LocalDate nextCouponDate) {
  public CreateInstrumentRequest(
      String ticker, String name, int lotSize, BigDecimal minStep, String currency) {
    this(
        ticker,
        name,
        lotSize,
        minStep,
        currency,
        InstrumentType.STOCK,
        null,
        null,
        null,
        null,
        null);
  }
}
