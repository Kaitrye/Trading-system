package ru.trading.system.account;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.operation.AccountOperationService;
import ru.trading.system.operation.dto.AccountOperationHistoryResponse;

@Validated
@RestController
@RequestMapping("/api/v1/accounts")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AccountController {
  private final AccountService accountService;
  private final AccountOperationService accountOperationService;

  @GetMapping
  public List<AccountResponse> findMyAccounts(@AuthenticationPrincipal UUID userId) {
    return accountService.findForUser(userId);
  }

  @GetMapping("/operations")
  public AccountOperationHistoryResponse findMyOperations(
      @AuthenticationPrincipal UUID userId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return accountOperationService.findForUser(userId, page, size);
  }
}
