package ru.trading.system.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    Page<Trade> findByInstrumentSeccodeOrderByExecutedAtDesc(String ticker, Pageable pageable);

    Optional<Trade> findFirstByInstrumentSeccodeOrderByExecutedAtDesc(String ticker);

    @Query("""
            select min(t.price) as lowPrice, max(t.price) as highPrice,
                   sum(t.quantity) as volume, count(t) as tradeCount
            from Trade t
            where t.instrument.seccode = :ticker
              and t.executedAt >= :from
              and t.executedAt < :to
            """)
    TradeDailyStatistics findDailyStatistics(
            @Param("ticker") String ticker,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
