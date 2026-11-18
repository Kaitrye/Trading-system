package ru.trading.system.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.order.dto.OrderResponse;
import ru.trading.system.position.Position;
import ru.trading.system.position.PositionId;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.trade.TradeRepository;
import ru.trading.system.trade.TradeService;
import ru.trading.system.trade.dto.MarketSummaryResponse;
import ru.trading.system.trade.dto.MyTradeHistoryResponse;
import ru.trading.system.trade.dto.TradeHistoryResponse;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@SpringBootTest
@Transactional
class OrderMatchingIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private InstrumentRepository instrumentRepository;
  @Autowired private PositionRepository positionRepository;
  @Autowired private TradeRepository tradeRepository;
  @Autowired private TradeService tradeService;

  @Test
  void matchesOrdersAtRestingOrderPriceAndUpdatesPortfolios() {
    Instrument instrument =
        instrumentRepository.save(
            new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));
    User seller = userRepository.save(new User("seller@example.test", "hash", Role.USER));
    Account sellerAccount = accountRepository.save(new Account(seller, "RUB"));
    Position sellerPosition = positionRepository.save(new Position(sellerAccount, instrument));
    sellerPosition.credit(10);

    User buyer = userRepository.save(new User("buyer@example.test", "hash", Role.USER));
    Account buyerAccount = accountRepository.save(new Account(buyer, "RUB"));
    buyerAccount.credit(new BigDecimal("1200"));

    orderService.create(
        seller.getId(), new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("100"), 10));
    orderService.create(
        buyer.getId(), new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("110"), 10));

    assertEquals(1, tradeRepository.count());
    TradeHistoryResponse history = tradeService.findForInstrument("SBER", 0, 10);
    assertEquals(1, history.totalElements());
    assertEquals(1, history.totalPages());
    assertEquals(1, history.content().size());
    assertEquals(0, new BigDecimal("100").compareTo(history.content().getFirst().price()));
    assertEquals(10, history.content().getFirst().quantity());
    MyTradeHistoryResponse buyerTrades = tradeService.findForUser(buyer.getId(), 0, 10);
    assertEquals(1, buyerTrades.totalElements());
    assertEquals(OrderSide.BUY, buyerTrades.content().getFirst().side());
    MyTradeHistoryResponse sellerTrades = tradeService.findForUser(seller.getId(), 0, 10);
    assertEquals(OrderSide.SELL, sellerTrades.content().getFirst().side());
    MarketSummaryResponse summary = tradeService.findSummary("SBER");
    assertEquals(0, new BigDecimal("100").compareTo(summary.lastPrice()));
    assertEquals(0, new BigDecimal("100").compareTo(summary.lowPrice()));
    assertEquals(0, new BigDecimal("100").compareTo(summary.highPrice()));
    assertEquals(10, summary.volume());
    assertEquals(1, summary.tradeCount());
    assertEquals(new BigDecimal("200"), buyerAccount.getBalance());
    assertEquals(BigDecimal.ZERO, buyerAccount.getReservedBalance());
    assertEquals(new BigDecimal("1000"), sellerAccount.getBalance());
    assertEquals(
        10,
        positionRepository
            .findById(new PositionId(buyerAccount.getId(), "SBER"))
            .orElseThrow()
            .getQuantity());
    assertEquals(0, sellerPosition.getQuantity());
    assertEquals(0, sellerPosition.getReservedQuantity());
  }

  @Test
  void matchesTheBestAvailablePriceBeforeWorseOffers() {
    Instrument instrument =
        instrumentRepository.save(
            new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));
    User expensiveSeller =
        userRepository.save(new User("expensive-seller@example.test", "hash", Role.USER));
    Account expensiveSellerAccount = accountRepository.save(new Account(expensiveSeller, "RUB"));
    Position expensivePosition =
        positionRepository.save(new Position(expensiveSellerAccount, instrument));
    expensivePosition.credit(10);

    User bestSeller = userRepository.save(new User("best-seller@example.test", "hash", Role.USER));
    Account bestSellerAccount = accountRepository.save(new Account(bestSeller, "RUB"));
    Position bestPosition = positionRepository.save(new Position(bestSellerAccount, instrument));
    bestPosition.credit(10);

    User buyer = userRepository.save(new User("best-price-buyer@example.test", "hash", Role.USER));
    Account buyerAccount = accountRepository.save(new Account(buyer, "RUB"));
    buyerAccount.credit(new BigDecimal("2000"));

    orderService.create(
        expensiveSeller.getId(),
        new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("100"), 10));
    orderService.create(
        bestSeller.getId(),
        new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("90"), 10));
    orderService.create(
        buyer.getId(), new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("110"), 10));

    TradeHistoryResponse history = tradeService.findForInstrument("SBER", 0, 10);
    assertEquals(1, history.totalElements());
    assertEquals(0, new BigDecimal("90").compareTo(history.content().getFirst().price()));
    assertEquals(
        OrderStatus.NEW, orderService.findForUser(expensiveSeller.getId()).getFirst().status());
    assertEquals(
        OrderStatus.FILLED, orderService.findForUser(bestSeller.getId()).getFirst().status());
    assertEquals(BigDecimal.ZERO, expensiveSellerAccount.getBalance());
    assertEquals(new BigDecimal("900"), bestSellerAccount.getBalance());
    assertEquals(new BigDecimal("1100"), buyerAccount.getBalance());
  }

  @Test
  void usesTimePriorityForEqualPricesAndKeepsTheRemainderActive() {
    Instrument instrument =
        instrumentRepository.save(
            new Instrument("SBER", "Сбербанк", 5, new BigDecimal("0.01"), "RUB"));
    User firstSeller =
        userRepository.save(new User("first-seller@example.test", "hash", Role.USER));
    Account firstSellerAccount = accountRepository.save(new Account(firstSeller, "RUB"));
    Position firstPosition = positionRepository.save(new Position(firstSellerAccount, instrument));
    firstPosition.credit(10);

    User secondSeller =
        userRepository.save(new User("second-seller@example.test", "hash", Role.USER));
    Account secondSellerAccount = accountRepository.save(new Account(secondSeller, "RUB"));
    Position secondPosition =
        positionRepository.save(new Position(secondSellerAccount, instrument));
    secondPosition.credit(10);

    User buyer =
        userRepository.save(new User("time-priority-buyer@example.test", "hash", Role.USER));
    Account buyerAccount = accountRepository.save(new Account(buyer, "RUB"));
    buyerAccount.credit(new BigDecimal("2000"));

    orderService.create(
        firstSeller.getId(),
        new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("100"), 10));
    orderService.create(
        secondSeller.getId(),
        new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("100"), 10));
    OrderResponse buyerOrder =
        orderService.create(
            buyer.getId(),
            new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("110"), 15));

    assertEquals(2, tradeRepository.count());
    assertEquals(
        OrderStatus.FILLED, orderService.findForUser(firstSeller.getId()).getFirst().status());
    OrderResponse secondSellerOrder = orderService.findForUser(secondSeller.getId()).getFirst();
    assertEquals(OrderStatus.PARTIALLY_FILLED, secondSellerOrder.status());
    assertEquals(5, secondSellerOrder.remainingQuantity());
    assertEquals(OrderStatus.FILLED, buyerOrder.status());
    assertEquals(0, buyerOrder.remainingQuantity());
    assertEquals(0, firstPosition.getQuantity());
    assertEquals(0, firstPosition.getReservedQuantity());
    assertEquals(5, secondPosition.getQuantity());
    assertEquals(5, secondPosition.getReservedQuantity());
    assertEquals(
        15,
        positionRepository
            .findById(new PositionId(buyerAccount.getId(), "SBER"))
            .orElseThrow()
            .getQuantity());
  }

  @Test
  void exchangesBaseAndQuoteCurrenciesForCurrencyPair() {
    Instrument pair =
        instrumentRepository.save(
            new Instrument(
                "USDRUB",
                "US Dollar / Russian Ruble",
                1,
                new BigDecimal("0.0001"),
                "RUB",
                ru.trading.system.instrument.InstrumentType.CURRENCY_PAIR,
                null,
                null,
                null,
                null,
                null,
                "USD",
                "RUB"));
    User seller = userRepository.save(new User("fx-seller@example.test", "hash", Role.USER));
    Account sellerRub = accountRepository.save(new Account(seller, "RUB"));
    Account sellerUsd = accountRepository.save(new Account(seller, "USD"));
    sellerUsd.credit(new BigDecimal("10"));

    User buyer = userRepository.save(new User("fx-buyer@example.test", "hash", Role.USER));
    Account buyerRub = accountRepository.save(new Account(buyer, "RUB"));
    Account buyerUsd = accountRepository.save(new Account(buyer, "USD"));
    buyerRub.credit(new BigDecimal("1000"));

    orderService.create(
        seller.getId(),
        new CreateOrderRequest(pair.getSeccode(), OrderSide.SELL, new BigDecimal("90"), 10));
    orderService.create(
        buyer.getId(),
        new CreateOrderRequest(pair.getSeccode(), OrderSide.BUY, new BigDecimal("100"), 10));

    assertEquals(0, BigDecimal.ZERO.compareTo(sellerUsd.getBalance()));
    assertEquals(0, new BigDecimal("900").compareTo(sellerRub.getBalance()));
    assertEquals(0, new BigDecimal("100").compareTo(buyerRub.getBalance()));
    assertEquals(0, new BigDecimal("10").compareTo(buyerUsd.getBalance()));
    assertEquals(1, tradeRepository.count());
  }
}
