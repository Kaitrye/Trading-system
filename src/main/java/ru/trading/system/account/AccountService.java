package ru.trading.system.account;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.user.User;

@Service
@RequiredArgsConstructor
public class AccountService {

  private static final String RUB = "RUB";

  private final AccountRepository accountRepository;

  public void createDefaultAccount(User user) {
    accountRepository.save(new Account(user, RUB));
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
