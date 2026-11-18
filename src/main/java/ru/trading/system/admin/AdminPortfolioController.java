package ru.trading.system.admin;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.account.AccountService;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.account.dto.DepositRequest;
import ru.trading.system.position.PositionService;
import ru.trading.system.position.dto.GrantPositionRequest;
import ru.trading.system.position.dto.PositionResponse;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminPortfolioController {
  private final AccountService accountService;
  private final PositionService positionService;

  @PostMapping("/accounts/{accountId}/deposit")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse deposit(
      @PathVariable UUID accountId, @Valid @RequestBody DepositRequest request) {
    return accountService.deposit(accountId, request.amount());
  }

  @PostMapping("/positions")
  @ResponseStatus(HttpStatus.CREATED)
  public PositionResponse grantPosition(@Valid @RequestBody GrantPositionRequest request) {
    return positionService.grant(request.accountId(), request.ticker(), request.quantity());
  }
}
