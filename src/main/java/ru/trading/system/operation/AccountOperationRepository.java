package ru.trading.system.operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountOperationRepository extends JpaRepository<AccountOperation, UUID> {
    Page<AccountOperation> findByAccountUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
