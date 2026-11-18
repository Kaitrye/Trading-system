package ru.trading.system.account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.operation.AccountOperation;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.OperationType;
import ru.trading.system.order.OrderRepository;
import ru.trading.system.order.OrderStatus;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AccountService {
  private static final String RUB = "RUB";
  private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
      List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final AccountOperationRepository accountOperationRepository;
  private final OrderRepository orderRepository;
  private final PositionRepository positionRepository;

  public void createDefaultAccount(User user) {
    accountRepository.save(new Account(user, RUB));
  }

  @Transactional
  public AccountResponse create(UUID userId, String currency) {
    String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
    if (accountRepository.findByUserIdAndCurrency(userId, normalizedCurrency).isPresent()) {
      throw new IllegalArgumentException("Account already exists");
    }
    Account account =
        accountRepository.save(
            new Account(userRepository.getReferenceById(userId), normalizedCurrency));
    return toResponse(account);
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> findForUser(UUID userId) {
    return accountRepository.findByUserIdOrderByCurrencyAsc(userId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public AccountResponse deposit(UUID accountId, BigDecimal amount) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    ensureActive(account);
    account.credit(amount);
    accountOperationRepository.save(
        new AccountOperation(account, null, OperationType.ADMIN_DEPOSIT, amount));
    return toResponse(account);
  }

  @Transactional
  public AccountResponse close(UUID userId, UUID accountId) {
    Account account =
        accountRepository
            .lockByIdAndUserId(accountId, userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    ensureActive(account);
    if (account.getBalance().compareTo(BigDecimal.ZERO) != 0
        || account.getReservedBalance().compareTo(BigDecimal.ZERO) != 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account balance must be zero");
    }
    if (orderRepository.existsByAccountIdAndStatusIn(accountId, ACTIVE_ORDER_STATUSES)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account has active orders");
    }
    if (positionRepository.existsByAccountIdAndQuantityGreaterThan(accountId, 0)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account has nonzero positions");
    }
    account.close();
    return toResponse(account);
  }

  private void ensureActive(Account account) {
    if (!account.isActive()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is closed");
    }
  }

  private AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getCurrency(),
        account.getBalance(),
        account.getReservedBalance(),
        account.getBalance().subtract(account.getReservedBalance()),
        account.getStatus());
  }
}
