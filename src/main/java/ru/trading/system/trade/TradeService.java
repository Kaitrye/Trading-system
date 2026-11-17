package ru.trading.system.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.trade.dto.TradeHistoryResponse;
import ru.trading.system.trade.dto.TradeResponse;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;

    @Transactional(readOnly = true)
    public TradeHistoryResponse findForInstrument(String rawTicker, int page, int size) {
        String ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
        if (!instrumentRepository.existsById(ticker)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found");
        }

        Page<TradeResponse> tradePage = tradeRepository
                .findByInstrumentSeccodeOrderByExecutedAtDesc(ticker, PageRequest.of(page, size))
                .map(trade -> new TradeResponse(
                        trade.getId(),
                        trade.getInstrument().getSeccode(),
                        trade.getPrice(),
                        trade.getQuantity(),
                        trade.getExecutedAt()
                ));

        return new TradeHistoryResponse(
                tradePage.getContent(),
                tradePage.getNumber(),
                tradePage.getSize(),
                tradePage.getTotalElements(),
                tradePage.getTotalPages()
        );
    }
}
