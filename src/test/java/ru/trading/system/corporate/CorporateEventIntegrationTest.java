package ru.trading.system.corporate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.instrument.InstrumentType;
import ru.trading.system.operation.AccountOperation;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.operation.OperationType;
import ru.trading.system.order.OrderService;
import ru.trading.system.order.OrderSide;
import ru.trading.system.order.OrderStatus;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.order.dto.OrderResponse;
import ru.trading.system.position.Position;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CorporateEventIntegrationTest {

    @Autowired
    private CorporateEventService corporateEventService;
    @Autowired
    private InstrumentRepository instrumentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PositionRepository positionRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private AccountOperationRepository accountOperationRepository;

    @Test
    void redeemsBondCancelsOrdersReleasesReservesAndRecordsOperations() {
        LocalDate maturityDate = LocalDate.of(2026, 5, 15);
        Instrument bond = instrumentRepository.save(new Instrument(
                "OFZ26238", "ОФЗ 26238", 1, new BigDecimal("0.01"), "RUB",
                InstrumentType.BOND, new BigDecimal("1000"), maturityDate,
                new BigDecimal("12"), 6, maturityDate
        ));
        User holder = userRepository.save(new User("bond-holder@example.test", "hash", Role.USER));
        Account holderAccount = accountRepository.save(new Account(holder, "RUB"));
        Position holderPosition = positionRepository.save(new Position(holderAccount, bond));
        holderPosition.credit(2);

        User buyer = userRepository.save(new User("bond-buyer@example.test", "hash", Role.USER));
        Account buyerAccount = accountRepository.save(new Account(buyer, "RUB"));
        buyerAccount.credit(new BigDecimal("100"));
        OrderResponse order = orderService.create(
                buyer.getId(), new CreateOrderRequest("OFZ26238", OrderSide.BUY, new BigDecimal("5"), 10)
        );

        int processed = corporateEventService.processEvents(maturityDate);

        assertEquals(1, processed);
        assertFalse(bond.isActive());
        assertEquals(0, holderPosition.getQuantity());
        assertEquals(new BigDecimal("2120.0000"), holderAccount.getBalance());
        assertEquals(new BigDecimal("100"), buyerAccount.getBalance());
        assertEquals(BigDecimal.ZERO, buyerAccount.getReservedBalance());
        assertEquals(OrderStatus.CANCELLED, orderService.findForUser(buyer.getId()).getFirst().status());

        List<AccountOperation> operations = accountOperationRepository
                .findByAccountUserIdOrderByCreatedAtDesc(holder.getId(), PageRequest.of(0, 10))
                .getContent();
        assertEquals(2, operations.size());
        assertTrue(operations.stream().anyMatch(operation ->
                operation.getOperationType() == OperationType.COUPON_PAYMENT
                        && operation.getAmount().compareTo(new BigDecimal("120.0000")) == 0
        ));
        assertTrue(operations.stream().anyMatch(operation ->
                operation.getOperationType() == OperationType.BOND_REDEMPTION
                        && operation.getAmount().compareTo(new BigDecimal("2000.0000")) == 0
        ));
    }
}
