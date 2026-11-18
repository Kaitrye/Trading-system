package ru.trading.system.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.account.AccountService;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.OperationType;
import ru.trading.system.position.PositionService;
import ru.trading.system.position.dto.PositionResponse;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@SpringBootTest
@Transactional
class AdminPortfolioIntegrationTest {

  @Autowired private AccountService accountService;
  @Autowired private PositionService positionService;
  @Autowired private UserRepository userRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private InstrumentRepository instrumentRepository;
  @Autowired private AccountOperationRepository accountOperationRepository;

  @Test
  void depositsMoneyAndRecordsAdministrativeOperation() {
    User user = userRepository.save(new User("deposit-user@example.test", "hash", Role.USER));
    Account account = accountRepository.save(new Account(user, "USD"));

    AccountResponse response = accountService.deposit(account.getId(), new BigDecimal("1250.50"));

    assertEquals(0, new BigDecimal("1250.50").compareTo(response.balance()));
    assertEquals(0, BigDecimal.ZERO.compareTo(response.reservedBalance()));
    assertEquals(1, accountOperationRepository.count());
    assertEquals(
        OperationType.ADMIN_DEPOSIT,
        accountOperationRepository.findAll().getFirst().getOperationType());
  }

  @Test
  void grantsSecuritiesInWholeLots() {
    User user = userRepository.save(new User("position-user@example.test", "hash", Role.USER));
    Account account = accountRepository.save(new Account(user, "RUB"));
    instrumentRepository.save(
        new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));

    PositionResponse response = positionService.grant(account.getId(), "sber", 20);

    assertEquals("SBER", response.ticker());
    assertEquals(20, response.quantity());
    assertEquals(20, response.availableQuantity());
  }
}
