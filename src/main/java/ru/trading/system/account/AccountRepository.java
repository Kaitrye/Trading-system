package ru.trading.system.account;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  List<Account> findByUserIdOrderByCurrencyAsc(UUID userId);

  Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id = :accountId and a.user.id = :userId")
  Optional<Account> lockByIdAndUserId(
      @Param("accountId") UUID accountId, @Param("userId") UUID userId);
}
