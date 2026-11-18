package ru.trading.system.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  List<Account> findByUserIdOrderByCurrencyAsc(UUID userId);

  Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);
}
