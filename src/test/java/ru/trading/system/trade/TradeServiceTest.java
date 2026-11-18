package ru.trading.system.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.Instrument;
import ru.trading.system.instrument.InstrumentRepository;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

  @Mock private TradeRepository tradeRepository;
  @Mock private InstrumentRepository instrumentRepository;

  @InjectMocks private TradeService tradeService;

  @Test
  void returnsDailySummaryWithLastPriceAndStatistics() {
    TradeDailyStatistics statistics = mock(TradeDailyStatistics.class);
    when(instrumentRepository.existsById("SBER")).thenReturn(true);
    when(statistics.getLowPrice()).thenReturn(new BigDecimal("90"));
    when(statistics.getHighPrice()).thenReturn(new BigDecimal("110"));
    when(statistics.getVolume()).thenReturn(50L);
    when(statistics.getTradeCount()).thenReturn(3L);
    when(tradeRepository.findDailyStatistics(org.mockito.ArgumentMatchers.eq("SBER"), any(), any()))
        .thenReturn(statistics);
    Trade lastTrade = mock(Trade.class);
    when(lastTrade.getPrice()).thenReturn(new BigDecimal("105"));
    when(tradeRepository.findFirstByInstrumentSeccodeOrderByExecutedAtDesc("SBER"))
        .thenReturn(Optional.of(lastTrade));

    var summary = tradeService.findSummary(" sber ");

    assertEquals(0, new BigDecimal("105").compareTo(summary.lastPrice()));
    assertEquals(0, new BigDecimal("90").compareTo(summary.lowPrice()));
    assertEquals(0, new BigDecimal("110").compareTo(summary.highPrice()));
    assertEquals(50, summary.volume());
    assertEquals(3, summary.tradeCount());
  }

  @Test
  void returnsNullPricesAndZeroesWhenNoTradesExistToday() {
    TradeDailyStatistics statistics = mock(TradeDailyStatistics.class);
    when(instrumentRepository.existsById("SBER")).thenReturn(true);
    when(tradeRepository.findDailyStatistics(org.mockito.ArgumentMatchers.eq("SBER"), any(), any()))
        .thenReturn(statistics);
    when(tradeRepository.findFirstByInstrumentSeccodeOrderByExecutedAtDesc("SBER"))
        .thenReturn(Optional.empty());

    var summary = tradeService.findSummary("SBER");

    assertNull(summary.lastPrice());
    assertNull(summary.lowPrice());
    assertNull(summary.highPrice());
    assertEquals(0, summary.volume());
    assertEquals(0, summary.tradeCount());
  }

  @Test
  void mapsPaginatedInstrumentHistory() {
    Trade trade = mock(Trade.class);
    Instrument instrument = mock(Instrument.class);
    OffsetDateTime executedAt = OffsetDateTime.now();
    when(instrumentRepository.existsById("SBER")).thenReturn(true);
    when(trade.getInstrument()).thenReturn(instrument);
    when(instrument.getSeccode()).thenReturn("SBER");
    when(trade.getPrice()).thenReturn(new BigDecimal("100"));
    when(trade.getQuantity()).thenReturn(10L);
    when(trade.getExecutedAt()).thenReturn(executedAt);
    when(tradeRepository.findByInstrumentSeccodeOrderByExecutedAtDesc(
            org.mockito.ArgumentMatchers.eq("SBER"), any()))
        .thenReturn(new PageImpl<>(List.of(trade)));

    var history = tradeService.findForInstrument("SBER", 0, 50);

    assertEquals(1, history.totalElements());
    assertEquals("SBER", history.content().getFirst().ticker());
    assertEquals(10, history.content().getFirst().quantity());
  }

  @Test
  void rejectsSummaryForUnknownInstrument() {
    when(instrumentRepository.existsById("SBER")).thenReturn(false);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> tradeService.findSummary("SBER"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(tradeRepository, org.mockito.Mockito.never())
        .findFirstByInstrumentSeccodeOrderByExecutedAtDesc(any());
  }
}
