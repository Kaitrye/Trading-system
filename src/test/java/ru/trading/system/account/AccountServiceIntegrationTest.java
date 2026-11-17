package ru.trading.system.account;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.dto.AccountResponse;
import ru.trading.system.auth.AuthService;
import ru.trading.system.user.dto.RegisterRequest;
import ru.trading.system.user.dto.UserResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Test
    void registrationCreatesEmptyRubleAccount() {
        UserResponse user = authService.register(new RegisterRequest("account-check@example.test", "password123"));

        List<AccountResponse> accounts = accountService.findForUser(user.id());

        assertEquals(1, accounts.size());
        AccountResponse account = accounts.getFirst();
        assertEquals("RUB", account.currency());
        assertEquals(0, account.balance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account.reservedBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, account.availableBalance().compareTo(BigDecimal.ZERO));
    }
}
