package ru.trading.system.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    Page<Trade> findByInstrumentSeccodeOrderByExecutedAtDesc(String ticker, Pageable pageable);
}
