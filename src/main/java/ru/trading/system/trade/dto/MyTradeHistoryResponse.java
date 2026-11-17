package ru.trading.system.trade.dto;

import java.util.List;

public record MyTradeHistoryResponse(
        List<MyTradeResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
