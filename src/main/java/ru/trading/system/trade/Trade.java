package ru.trading.system.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.order.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccode", nullable = false)
    private Instrument instrument;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private long quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_order_id", nullable = false)
    private Order sellOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime executedAt;

    public Trade(Instrument instrument, BigDecimal price, long quantity, Order buyOrder, Order sellOrder) {
        this.instrument = instrument;
        this.price = price;
        this.quantity = quantity;
        this.buyOrder = buyOrder;
        this.sellOrder = sellOrder;
    }
}
