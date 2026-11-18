package ru.trading.system.instrument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @Column(length = 16)
    private String seccode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstrumentType type;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int lotSize;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minStep;

    @JdbcTypeCode(Types.CHAR)
    @Column(nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(precision = 19, scale = 4)
    private BigDecimal nominal;

    private LocalDate maturityDate;

    @Column(precision = 7, scale = 4)
    private BigDecimal couponRate;

    private Integer couponFrequencyMonths;

    private LocalDate nextCouponDate;

    @Column(nullable = false)
    private boolean active;

    public Instrument(String seccode, String name, int lotSize, BigDecimal minStep, String currency) {
        this(seccode, name, lotSize, minStep, currency, InstrumentType.STOCK, null, null, null, null, null);
    }

    public Instrument(
            String seccode,
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
        this.seccode = seccode;
        this.name = name;
        this.lotSize = lotSize;
        this.minStep = minStep;
        this.currency = currency;
        this.type = type;
        this.nominal = nominal;
        this.maturityDate = maturityDate;
        this.couponRate = couponRate;
        this.couponFrequencyMonths = couponFrequencyMonths;
        this.nextCouponDate = nextCouponDate;
        this.active = true;
    }

    public void changeStatus(boolean active) {
        this.active = active;
    }

    public void advanceCouponDate() {
        nextCouponDate = nextCouponDate.plusMonths(couponFrequencyMonths);
    }

    public void deactivate() {
        active = false;
    }
}
