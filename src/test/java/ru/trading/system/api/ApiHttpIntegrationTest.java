package ru.trading.system.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;

    @Test
    void registersLogsInAndRequiresBearerTokenForAccounts() throws Exception {
        String email = "api-user@example.test";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));

        String token = login(email);

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currency").value("RUB"))
                .andExpect(jsonPath("$[0].balance").value(0));
    }

    @Test
    void keepsOrdersPrivateAndRejectsForeignCancellation() throws Exception {
        String ownerEmail = "order-owner@example.test";
        String outsiderEmail = "order-outsider@example.test";
        String ownerToken = registerAndLogin(ownerEmail);
        String outsiderToken = registerAndLogin(outsiderEmail);
        User owner = userRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow();
        Account ownerAccount = accountRepository.findByUserIdAndCurrency(owner.getId(), "RUB").orElseThrow();
        ownerAccount.credit(new BigDecimal("1000"));
        instrumentRepository.save(new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyOrderJson(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();
        UUID orderId = UUID.fromString(readStringField(result, "id"));

        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(delete("/api/v1/orders/{orderId}", orderId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()));
    }

    @Test
    void returnsAStableErrorForOrderWithInvalidLotQuantity() throws Exception {
        String email = "invalid-lot@example.test";
        String token = registerAndLogin(email);
        instrumentRepository.save(new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buyOrderJson(5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LOT_QUANTITY"))
                .andExpect(jsonPath("$.message").value("Quantity must be a multiple of lot size"));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(email)))
                .andExpect(status().isCreated());
        return login(email);
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        return readStringField(result, "accessToken");
    }

    private String credentialsJson(String email) {
        return "{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email);
    }

    private String buyOrderJson(long quantity) {
        return "{\"ticker\":\"SBER\",\"side\":\"BUY\",\"price\":100,\"quantity\":%d}".formatted(quantity);
    }

    private String readStringField(MvcResult result, String field) throws Exception {
        String response = result.getResponse().getContentAsString();
        return Pattern.compile("\"" + field + "\":\"([^\"]+)\"")
                .matcher(response)
                .results()
                .findFirst()
                .orElseThrow()
                .group(1);
    }
}
