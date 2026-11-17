package ru.trading.system.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.order.dto.OrderResponse;
import ru.trading.system.operation.AccountOperation;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.OperationType;
import ru.trading.system.position.Position;
import ru.trading.system.position.PositionId;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.trade.Trade;
import ru.trading.system.trade.TradeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String RUB = "RUB";

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final AccountOperationRepository accountOperationRepository;

    @Transactional
    public OrderResponse create(UUID userId, CreateOrderRequest request) {
        String ticker = request.ticker().trim().toUpperCase(Locale.ROOT);
        Instrument instrument = instrumentRepository.findBySeccodeAndActiveTrue(ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
        if (request.quantity() % instrument.getLotSize() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a multiple of lot size");
        }
        if (request.price().remainder(instrument.getMinStep()).compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be a multiple of minimum step");
        }
        if (!RUB.equals(instrument.getCurrency().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only RUB instruments are supported");
        }

        Account account = accountRepository.findByUserIdAndCurrency(userId, RUB)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "RUB account not found"));

        reserveForOrder(account, instrument, request);
        Order order = orderRepository.saveAndFlush(new Order(account, instrument, request.side(), request.price(), request.quantity()));
        match(order);
        return toResponse(order);
    }

    private void reserveForOrder(Account account, Instrument instrument, CreateOrderRequest request) {
        if (request.side() == OrderSide.BUY) {
            BigDecimal amount = request.price().multiply(BigDecimal.valueOf(request.quantity()));
            if (!account.reserve(amount)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient available balance");
            }
            return;
        }

        Position position = positionRepository.findById(new PositionId(account.getId(), instrument.getSeccode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Position not found"));
        if (!position.reserve(request.quantity())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient available shares");
        }
    }

    private void match(Order incomingOrder) {
        List<Order> matchingOrders = incomingOrder.getSide() == OrderSide.BUY
                ? orderRepository.findMatchingSellOrders(incomingOrder.getInstrument().getSeccode(), incomingOrder.getAccount().getId(),
                        OrderSide.SELL, List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED), incomingOrder.getPrice())
                : orderRepository.findMatchingBuyOrders(incomingOrder.getInstrument().getSeccode(), incomingOrder.getAccount().getId(),
                        OrderSide.BUY, List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED), incomingOrder.getPrice());

        for (Order restingOrder : matchingOrders) {
            if (!incomingOrder.isActive()) {
                break;
            }
            long quantity = Math.min(incomingOrder.getRemainingQuantity(), restingOrder.getRemainingQuantity());
            Order buyOrder = incomingOrder.getSide() == OrderSide.BUY ? incomingOrder : restingOrder;
            Order sellOrder = incomingOrder.getSide() == OrderSide.SELL ? incomingOrder : restingOrder;
            executeTrade(buyOrder, sellOrder, restingOrder.getPrice(), quantity);
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder, BigDecimal price, long quantity) {
        BigDecimal reservedAmount = buyOrder.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));
        buyOrder.getAccount().executeBuy(reservedAmount, cost);
        sellOrder.getAccount().credit(cost);

        PositionId buyerPositionId = new PositionId(buyOrder.getAccount().getId(), buyOrder.getInstrument().getSeccode());
        Position buyerPosition = positionRepository.findById(buyerPositionId)
                .orElseGet(() -> positionRepository.save(new Position(buyOrder.getAccount(), buyOrder.getInstrument())));
        buyerPosition.credit(quantity);

        Position sellerPosition = positionRepository.findById(new PositionId(sellOrder.getAccount().getId(), sellOrder.getInstrument().getSeccode()))
                .orElseThrow(() -> new IllegalStateException("Reserved seller position not found"));
        sellerPosition.executeSell(quantity);

        buyOrder.fill(quantity);
        sellOrder.fill(quantity);
        Trade trade = tradeRepository.save(new Trade(buyOrder.getInstrument(), price, quantity, buyOrder, sellOrder));
        accountOperationRepository.saveAll(List.of(
                new AccountOperation(buyOrder.getAccount(), trade, OperationType.TRADE_BUY, cost.negate()),
                new AccountOperation(sellOrder.getAccount(), trade, OperationType.TRADE_SELL, cost)
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findForUser(UUID userId) {
        return orderRepository.findByAccountUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancel(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndAccountUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is no longer active");
        }

        if (order.getSide() == OrderSide.BUY) {
            order.getAccount().release(order.getPrice().multiply(BigDecimal.valueOf(order.getRemainingQuantity())));
        } else {
            Position position = positionRepository.findById(new PositionId(order.getAccount().getId(), order.getInstrument().getSeccode()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Position not found"));
            position.release(order.getRemainingQuantity());
        }
        order.cancel();
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getAccount().getId(), order.getInstrument().getSeccode(),
                order.getSide(), order.getPrice(), order.getQuantity(), order.getRemainingQuantity(),
                order.getStatus(), order.getCreatedAt());
    }
}
