package ru.trading.system.instrument;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @Column(length = 16)
    private String seccode;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int lotSize;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minStep;

    @JdbcTypeCode(Types.CHAR)
    @Column(nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(nullable = false)
    private boolean active;

    public Instrument(String seccode, String name, int lotSize, BigDecimal minStep, String currency) { this.seccode = seccode; this.type = "STOCK"; this.name = name; this.lotSize = lotSize; this.minStep = minStep; this.currency = currency; this.active = true; }

    public void changeStatus(boolean active) { this.active = active; }
}
