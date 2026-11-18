package ru.trading.system.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@SpringBootTest
class ParallelOrdersSmokeIntegrationTest {
  private static final int PARTICIPANTS = 8;
  private static final long QUANTITY = 10;
  private static final BigDecimal PRICE = new BigDecimal("100");

  private final String ticker =
      "SMK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  private final String emailPrefix = "smoke-" + UUID.randomUUID();

  @Autowired private OrderService orderService;
  @Autowired private InstrumentRepository instrumentRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private PositionRepository positionRepository;
  @Autowired private TradeRepository tradeRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void removeSmokeData() {
    jdbcTemplate.update(
        "DELETE FROM account_operations WHERE trade_id IN (SELECT id FROM trades WHERE seccode = ?)",
        ticker);
    jdbcTemplate.update("DELETE FROM trades WHERE seccode = ?", ticker);
    jdbcTemplate.update("DELETE FROM orders WHERE seccode = ?", ticker);
    jdbcTemplate.update("DELETE FROM positions WHERE seccode = ?", ticker);
    jdbcTemplate.update("DELETE FROM instruments WHERE seccode = ?", ticker);
    jdbcTemplate.update(
        "DELETE FROM accounts WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)",
        emailPrefix + "%");
    jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", emailPrefix + "%");
  }

  @Test
  void processesParallelOrdersForOneInstrumentWithoutBreakingPortfolios() throws Exception {
    Instrument instrument =
        instrumentRepository.save(
            new Instrument(ticker, "Smoke instrument", 1, new BigDecimal("0.01"), "RUB"));
    List<Participant> sellers = createSellers(instrument);
    List<Participant> buyers = createBuyers();

    try (ExecutorService executor = Executors.newFixedThreadPool(PARTICIPANTS)) {
      runConcurrently(executor, sellers, OrderSide.SELL, OrderStatus.NEW);
      runConcurrently(executor, buyers, OrderSide.BUY, OrderStatus.FILLED);
    }

    assertEquals(
        PARTICIPANTS,
        tradeRepository
            .findByInstrumentSeccodeOrderByExecutedAtDesc(
                ticker, org.springframework.data.domain.PageRequest.of(0, 100))
            .getTotalElements());
    assertPortfolios(sellers, buyers);
  }

  private List<Participant> createSellers(Instrument instrument) {
    List<Participant> sellers = new ArrayList<>();
    for (int index = 0; index < PARTICIPANTS; index++) {
      User user =
          userRepository.save(
              new User(emailPrefix + "-seller-" + index + "@example.test", "hash", Role.USER));
      Account account = accountRepository.save(new Account(user, "RUB"));
      Position position = positionRepository.save(new Position(account, instrument));
      position.credit(QUANTITY);
      positionRepository.save(position);
      sellers.add(new Participant(user.getId(), account.getId()));
    }
    return sellers;
  }

  private List<Participant> createBuyers() {
    List<Participant> buyers = new ArrayList<>();
    for (int index = 0; index < PARTICIPANTS; index++) {
      User user =
          userRepository.save(
              new User(emailPrefix + "-buyer-" + index + "@example.test", "hash", Role.USER));
      Account account = accountRepository.save(new Account(user, "RUB"));
      account.credit(new BigDecimal("2000"));
      accountRepository.save(account);
      buyers.add(new Participant(user.getId(), account.getId()));
    }
    return buyers;
  }

  private void runConcurrently(
      ExecutorService executor,
      List<Participant> participants,
      OrderSide side,
      OrderStatus expectedStatus)
      throws Exception {
    CountDownLatch ready = new CountDownLatch(participants.size());
    CountDownLatch start = new CountDownLatch(1);
    List<Future<OrderResponse>> futures =
        participants.stream()
            .map(
                participant ->
                    executor.submit(
                        () -> {
                          ready.countDown();
                          assertTrue(start.await(10, TimeUnit.SECONDS));
                          return orderService.create(
                              participant.userId(),
                              new CreateOrderRequest(ticker, side, PRICE, QUANTITY));
                        }))
            .toList();

    assertTrue(ready.await(10, TimeUnit.SECONDS));
    start.countDown();
    for (Future<OrderResponse> future : futures) {
      assertEquals(expectedStatus, future.get(30, TimeUnit.SECONDS).status());
    }
  }

  private void assertPortfolios(List<Participant> sellers, List<Participant> buyers) {
    for (Participant seller : sellers) {
      Account account = accountRepository.findById(seller.accountId()).orElseThrow();
      Position position =
          positionRepository.findById(new PositionId(seller.accountId(), ticker)).orElseThrow();
      assertEquals(0, new BigDecimal("1000").compareTo(account.getBalance()));
      assertEquals(0, BigDecimal.ZERO.compareTo(account.getReservedBalance()));
      assertEquals(0, position.getQuantity());
      assertEquals(0, position.getReservedQuantity());
    }
    for (Participant buyer : buyers) {
      Account account = accountRepository.findById(buyer.accountId()).orElseThrow();
      Position position =
          positionRepository.findById(new PositionId(buyer.accountId(), ticker)).orElseThrow();
      assertEquals(0, new BigDecimal("1000").compareTo(account.getBalance()));
      assertEquals(0, BigDecimal.ZERO.compareTo(account.getReservedBalance()));
      assertEquals(QUANTITY, position.getQuantity());
      assertEquals(0, position.getReservedQuantity());
    }
  }

  private record Participant(UUID userId, UUID accountId) {}
}
