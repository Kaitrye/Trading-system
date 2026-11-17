package ru.trading.system.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.order.dto.OrderBookLevelResponse;
import ru.trading.system.order.dto.OrderBookResponse;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderBookService {
    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    private final OrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;

    @Transactional(readOnly = true)
    public OrderBookResponse getOrderBook(String rawTicker) {
        String ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
        if (!instrumentRepository.existsById(ticker)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found");
        }

        return new OrderBookResponse(
                ticker,
                toResponse(orderRepository.findBuyLevels(ticker, OrderSide.BUY, ACTIVE_STATUSES)),
                toResponse(orderRepository.findSellLevels(ticker, OrderSide.SELL, ACTIVE_STATUSES))
        );
    }

    private List<OrderBookLevelResponse> toResponse(List<OrderBookLevel> levels) {
        return levels.stream()
                .map(level -> new OrderBookLevelResponse(level.getPrice(), level.getQuantity()))
                .toList();
    }
}
