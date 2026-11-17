package ru.trading.system.trade.dto;

import java.math.BigDecimal;

public record MarketSummaryResponse(
        String ticker,
        BigDecimal lastPrice,
        BigDecimal lowPrice,
        BigDecimal highPrice,
        long volume,
        long tradeCount
) {
}
