package ru.trading.system.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Sql("/sql/seed_instruments.sql")
class InstrumentRepositoryIntegrationTest {

  @Autowired private InstrumentRepository instrumentRepository;

  @Test
  void returnsActiveInstrumentsOrderedByTicker() {
    List<String> tickers =
        instrumentRepository.findByActiveTrueOrderBySeccodeAsc().stream()
            .map(Instrument::getSeccode)
            .toList();

    assertEquals(List.of("GAZP", "LKOH", "SBER"), tickers);
  }

  @Test
  void persistsBondDetails() {
    Instrument bond =
        instrumentRepository.save(
            new Instrument(
                "OFZ26238",
                "ОФЗ 26238",
                1,
                new BigDecimal("0.01"),
                "RUB",
                InstrumentType.BOND,
                new BigDecimal("1000.00"),
                LocalDate.of(2041, 5, 15),
                new BigDecimal("12.5000"),
                6,
                LocalDate.of(2027, 5, 15)));

    Instrument savedBond = instrumentRepository.findById(bond.getSeccode()).orElseThrow();

    assertEquals(InstrumentType.BOND, savedBond.getType());
    assertEquals(new BigDecimal("1000.0000"), savedBond.getNominal());
    assertEquals(LocalDate.of(2041, 5, 15), savedBond.getMaturityDate());
    assertEquals(new BigDecimal("12.5000"), savedBond.getCouponRate());
    assertEquals(6, savedBond.getCouponFrequencyMonths());
    assertEquals(LocalDate.of(2027, 5, 15), savedBond.getNextCouponDate());
  }
}
