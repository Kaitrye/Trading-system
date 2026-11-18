package ru.trading.system.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.Account;
import ru.trading.system.account.AccountRepository;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.operation.AccountOperationRepository;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.position.PositionRepository;
import ru.trading.system.trade.TradeRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private AccountOperationRepository accountOperationRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void rejectsQuantityThatIsNotAMultipleOfLotSize() {
        when(instrumentRepository.findBySeccodeAndActiveTrue("SBER"))
                .thenReturn(Optional.of(new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.create(UUID.randomUUID(), new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("100"), 15))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(accountRepository, never()).findByUserIdAndCurrency(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        verify(orderRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBuyOrderWhenAvailableBalanceIsInsufficient() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(null, "RUB");
        account.credit(new BigDecimal("100"));
        when(instrumentRepository.findBySeccodeAndActiveTrue("SBER"))
                .thenReturn(Optional.of(new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB")));
        when(accountRepository.findByUserIdAndCurrency(userId, "RUB")).thenReturn(Optional.of(account));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                orderService.create(userId, new CreateOrderRequest("SBER", OrderSide.BUY, new BigDecimal("20"), 10))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancelsActiveBuyOrdersForInstrumentAndReleasesReservedBalance() {
        Account account = new Account(null, "RUB");
        account.credit(new BigDecimal("100"));
        account.reserve(new BigDecimal("50"));
        Instrument instrument = new Instrument("OFZ26238", "ОФЗ 26238", 1, new BigDecimal("0.01"), "RUB");
        Order order = new Order(account, instrument, OrderSide.BUY, new BigDecimal("5"), 10);
        when(orderRepository.lockActiveOrdersByInstrument(
                "OFZ26238", java.util.List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED)
        )).thenReturn(java.util.List.of(order));

        int cancelled = orderService.cancelActiveOrdersForInstrument("OFZ26238");

        assertEquals(1, cancelled);
        assertEquals(new BigDecimal("0"), account.getReservedBalance());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
