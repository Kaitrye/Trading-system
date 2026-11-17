package ru.trading.system.instrument;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@Sql("/sql/seed_instruments.sql")
class InstrumentRepositoryIntegrationTest {

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Test
    void returnsActiveInstrumentsOrderedByTicker() {
        List<String> tickers = instrumentRepository.findByActiveTrueOrderBySeccodeAsc().stream()
                .map(Instrument::getSeccode)
                .toList();

        assertEquals(List.of("GAZP", "LKOH", "SBER"), tickers);
    }
}
