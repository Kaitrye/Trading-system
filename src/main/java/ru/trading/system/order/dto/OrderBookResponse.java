package ru.trading.system.order.dto;

import java.util.List;

public record OrderBookResponse(
    String ticker, List<OrderBookLevelResponse> bids, List<OrderBookLevelResponse> asks) {}
