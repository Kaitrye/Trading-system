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
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AccountService {
  private static final String RUB = "RUB";
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final AccountOperationRepository accountOperationRepository;

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
    account.credit(amount);
    accountOperationRepository.save(
        new AccountOperation(account, null, OperationType.ADMIN_DEPOSIT, amount));
    return toResponse(account);
  }

  private AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getCurrency(),
        account.getBalance(),
        account.getReservedBalance(),
        account.getBalance().subtract(account.getReservedBalance()));
  }
}
