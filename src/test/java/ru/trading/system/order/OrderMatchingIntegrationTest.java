package ru.trading.system.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.AccountOperationService;
import ru.trading.system.operation.OperationType;
import ru.trading.system.operation.dto.AccountOperationHistoryResponse;
import ru.trading.system.position.Position;
import ru.trading.system.position.PositionId;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.trade.TradeRepository;
import ru.trading.system.trade.TradeService;
import ru.trading.system.trade.dto.TradeHistoryResponse;
import ru.trading.system.trade.dto.TradeResponse;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class OrderMatchingIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private AccountOperationRepository accountOperationRepository;
    @Autowired
    private AccountOperationService accountOperationService;

    @Test
    void matchesOrdersAtRestingOrderPriceAndUpdatesPortfolios() {
        Instrument instrument = instrumentRepository.save(new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB"));
        User seller = userRepository.save(new User("seller@example.test", "hash", Role.USER));
        Account sellerAccount = accountRepository.save(new Account(seller, "RUB"));
        Position sellerPosition = positionRepository.save(new Position(sellerAccount, instrument));
        sellerPosition.credit(10);

        User buyer = userRepository.save(new User("buyer@example.test", "hash", Role.USER));
        Account buyerAccount = accountRepository.save(new Account(buyer, "RUB"));
        buyerAccount.credit(new BigDecimal("1200"));

        orderService.create(seller.getId(), new CreateOrderRequest("SBER", OrderSide.SELL, new BigDecimal("100"), 10));
        orderService.create(buyer.getId(), new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("110"), 10));

        assertEquals(1, tradeRepository.count());
        TradeHistoryResponse history = tradeService.findForInstrument("SBER", 0, 10);
        assertEquals(1, history.totalElements());
        assertEquals(1, history.totalPages());
        assertEquals(1, history.content().size());
        assertEquals(0, new BigDecimal("100").compareTo(history.content().getFirst().price()));
        assertEquals(10, history.content().getFirst().quantity());
        assertEquals(new BigDecimal("200"), buyerAccount.getBalance());
        assertEquals(BigDecimal.ZERO, buyerAccount.getReservedBalance());
        assertEquals(new BigDecimal("1000"), sellerAccount.getBalance());
        assertEquals(10, positionRepository.findById(new PositionId(buyerAccount.getId(), "SBER")).orElseThrow().getQuantity());
        assertEquals(0, sellerPosition.getQuantity());
        assertEquals(0, sellerPosition.getReservedQuantity());
    }
}
