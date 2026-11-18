package ru.trading.system.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.dto.CreateInstrumentRequest;

@ExtendWith(MockitoExtension.class)
class InstrumentServiceTest {

  @Mock private InstrumentRepository instrumentRepository;

  @InjectMocks private InstrumentService instrumentService;

  @Test
  void createsNormalizedInstrument() {
    when(instrumentRepository.existsById("SBER")).thenReturn(false);
    when(instrumentRepository.save(any(Instrument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        instrumentService.create(
            new CreateInstrumentRequest(
                " sber ", " Сбербанк ", 10, new BigDecimal("0.01"), " rub "));

    assertEquals("SBER", response.ticker());
    assertEquals("Сбербанк", response.name());
    assertEquals("RUB", response.currency());
    assertEquals(InstrumentType.STOCK, response.type());
    verify(instrumentRepository).existsById("SBER");
  }

  @Test
  void createsBondWithCouponSchedule() {
    when(instrumentRepository.existsById("OFZ26238")).thenReturn(false);
    when(instrumentRepository.save(any(Instrument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        instrumentService.create(
            new CreateInstrumentRequest(
                "ofz26238",
                "ОФЗ 26238",
                1,
                new BigDecimal("0.01"),
                "RUB",
                InstrumentType.BOND,
                new BigDecimal("1000"),
                LocalDate.of(2041, 5, 15),
                new BigDecimal("12.50"),
                6,
                LocalDate.of(2027, 5, 15)));

    assertEquals(InstrumentType.BOND, response.type());
    assertEquals(new BigDecimal("1000"), response.nominal());
    assertEquals(LocalDate.of(2041, 5, 15), response.maturityDate());
    assertEquals(new BigDecimal("12.50"), response.couponRate());
    assertEquals(6, response.couponFrequencyMonths());
    assertEquals(LocalDate.of(2027, 5, 15), response.nextCouponDate());
  }

  @Test
  void rejectsDuplicateTicker() {
    when(instrumentRepository.existsById("SBER")).thenReturn(true);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                instrumentService.create(
                    new CreateInstrumentRequest(
                        "SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB")));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verify(instrumentRepository, never()).save(any());
  }

  @Test
  void findsActiveInstrumentUsingNormalizedTicker() {
    Instrument instrument = new Instrument("SBER", "Сбербанк", 10, new BigDecimal("0.01"), "RUB");
    when(instrumentRepository.findBySeccodeAndActiveTrue("SBER"))
        .thenReturn(Optional.of(instrument));

    var response = instrumentService.findByTicker(" sber ");

    assertEquals("SBER", response.ticker());
    verify(instrumentRepository).findBySeccodeAndActiveTrue("SBER");
  }

  @Test
  void rejectsUnknownTicker() {
    when(instrumentRepository.findBySeccodeAndActiveTrue("SBER")).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> instrumentService.findByTicker("SBER"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void createsCurrencyPairWithNormalizedCurrencies() {
    when(instrumentRepository.existsById("USDRUB")).thenReturn(false);
    when(instrumentRepository.save(any(Instrument.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        instrumentService.create(
            new CreateInstrumentRequest(
                "usdrub",
                "US Dollar / Russian Ruble",
                1,
                new BigDecimal("0.0001"),
                "rub",
                InstrumentType.CURRENCY_PAIR,
                null,
                null,
                null,
                null,
                null,
                "usd",
                "rub"));

    assertEquals(InstrumentType.CURRENCY_PAIR, response.type());
    assertEquals("USD", response.baseCurrency());
    assertEquals("RUB", response.quoteCurrency());
    assertEquals("RUB", response.currency());
  }

  @Test
  void rejectsCurrencyPairWithSameBaseAndQuoteCurrencies() {
    when(instrumentRepository.existsById("USDXUSD")).thenReturn(false);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                instrumentService.create(
                    new CreateInstrumentRequest(
                        "usdxusd",
                        "US Dollar / US Dollar",
                        1,
                        new BigDecimal("0.0001"),
                        "USD",
                        InstrumentType.CURRENCY_PAIR,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "USD",
                        "USD")));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }
}
