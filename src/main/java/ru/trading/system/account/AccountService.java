package ru.trading.system.account;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AccountService {
  private static final String RUB = "RUB";
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

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

  private AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getCurrency(),
        account.getBalance(),
        account.getReservedBalance(),
        account.getBalance().subtract(account.getReservedBalance()));
  }
}
