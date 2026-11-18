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
import ru.trading.system.order.dto.OrderBookResponse;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@SpringBootTest
@Transactional
class OrderBookServiceIntegrationTest {

  @Autowired private OrderBookService orderBookService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private InstrumentRepository instrumentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;

  @Test
  void groupsActiveOrdersByPriceLevel() {
    Instrument instrument =
        instrumentRepository.save(
            new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));
    Account account =
        accountRepository.save(
            new Account(
                userRepository.save(new User("book@example.test", "hash", Role.USER)), "RUB"));

    orderRepository.save(new Order(account, instrument, OrderSide.BUY, new BigDecimal("100"), 10));
    orderRepository.save(new Order(account, instrument, OrderSide.BUY, new BigDecimal("100"), 20));
    orderRepository.save(new Order(account, instrument, OrderSide.SELL, new BigDecimal("120"), 10));

    OrderBookResponse orderBook = orderBookService.getOrderBook("sber");

    assertEquals("SBER", orderBook.ticker());
    assertEquals(1, orderBook.bids().size());
    assertEquals(0, new BigDecimal("100").compareTo(orderBook.bids().getFirst().price()));
    assertEquals(30, orderBook.bids().getFirst().quantity());
    assertEquals(1, orderBook.asks().size());
    assertEquals(0, new BigDecimal("120").compareTo(orderBook.asks().getFirst().price()));
    assertEquals(10, orderBook.asks().getFirst().quantity());
  }
}
