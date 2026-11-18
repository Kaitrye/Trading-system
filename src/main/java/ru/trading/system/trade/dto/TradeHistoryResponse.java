package ru.trading.system.trade.dto;

import java.util.List;

public record TradeHistoryResponse(
    List<TradeResponse> content, int page, int size, long totalElements, int totalPages) {}
