package ru.trading.system.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.InstrumentRepository;

@ExtendWith(MockitoExtension.class)
class OrderBookServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private InstrumentRepository instrumentRepository;

  @InjectMocks private OrderBookService orderBookService;

  @Test
  void mapsBidAndAskLevels() {
    OrderBookLevel bid = mock(OrderBookLevel.class);
    OrderBookLevel ask = mock(OrderBookLevel.class);
    when(instrumentRepository.existsById("SBER")).thenReturn(true);
    when(bid.getPrice()).thenReturn(new BigDecimal("100"));
    when(bid.getQuantity()).thenReturn(20L);
    when(ask.getPrice()).thenReturn(new BigDecimal("101"));
    when(ask.getQuantity()).thenReturn(10L);
    when(orderRepository.findBuyLevels(
            org.mockito.ArgumentMatchers.eq("SBER"),
            org.mockito.ArgumentMatchers.eq(OrderSide.BUY),
            org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(bid));
    when(orderRepository.findSellLevels(
            org.mockito.ArgumentMatchers.eq("SBER"),
            org.mockito.ArgumentMatchers.eq(OrderSide.SELL),
            org.mockito.ArgumentMatchers.anyCollection()))
        .thenReturn(List.of(ask));

    var orderBook = orderBookService.getOrderBook(" sber ");

    assertEquals("SBER", orderBook.ticker());
    assertEquals(20, orderBook.bids().getFirst().quantity());
    assertEquals(10, orderBook.asks().getFirst().quantity());
  }

  @Test
  void rejectsUnknownInstrument() {
    when(instrumentRepository.existsById("SBER")).thenReturn(false);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> orderBookService.getOrderBook("SBER"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }
}
