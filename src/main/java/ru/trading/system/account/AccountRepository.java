package ru.trading.system.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCurrencyAsc(UUID userId);

    Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);
}
