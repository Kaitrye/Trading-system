package ru.trading.system.operation;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.operation.dto.AccountOperationHistoryResponse;
import ru.trading.system.operation.dto.AccountOperationResponse;

@Service
@RequiredArgsConstructor
public class AccountOperationService {
  private final AccountOperationRepository accountOperationRepository;

  @Transactional(readOnly = true)
  public AccountOperationHistoryResponse findForUser(UUID userId, int page, int size) {
    Page<AccountOperationResponse> operationPage =
        accountOperationRepository
            .findByAccountUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
            .map(
                operation ->
                    new AccountOperationResponse(
                        operation.getId(),
                        operation.getAccount().getId(),
                        operation.getTrade() == null ? null : operation.getTrade().getId(),
                        operation.getOperationType(),
                        operation.getAmount(),
                        operation.getCurrency().trim(),
                        operation.getCreatedAt()));

    return new AccountOperationHistoryResponse(
        operationPage.getContent(),
        operationPage.getNumber(),
        operationPage.getSize(),
        operationPage.getTotalElements(),
        operationPage.getTotalPages());
  }
}
