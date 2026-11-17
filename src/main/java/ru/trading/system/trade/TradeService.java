package ru.trading.system.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.instrument.InstrumentRepository;
import ru.trading.system.order.OrderSide;
import ru.trading.system.trade.dto.MarketSummaryResponse;
import ru.trading.system.trade.dto.MyTradeHistoryResponse;
import ru.trading.system.trade.dto.MyTradeResponse;
import ru.trading.system.trade.dto.TradeHistoryResponse;
import ru.trading.system.trade.dto.TradeResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("Europe/Moscow");

    private final InstrumentRepository instrumentRepository;

    @Transactional(readOnly = true)
    public MyTradeHistoryResponse findForUser(UUID userId, int page, int size) {
        Page<MyTradeResponse> tradePage = tradeRepository.findForUser(userId, PageRequest.of(page, size))
                .map(trade -> {
                    boolean isBuyer = trade.getBuyOrder().getAccount().getUser().getId().equals(userId);
                    return new MyTradeResponse(
                            trade.getId(),
                            isBuyer ? trade.getBuyOrder().getAccount().getId() : trade.getSellOrder().getAccount().getId(),
                            trade.getInstrument().getSeccode(),
                            isBuyer ? OrderSide.BUY : OrderSide.SELL,
                            trade.getPrice(),
                            trade.getQuantity(),
                            trade.getExecutedAt()
                    );
                });
        return new MyTradeHistoryResponse(
                tradePage.getContent(),
                tradePage.getNumber(),
                tradePage.getSize(),
                tradePage.getTotalElements(),
                tradePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public MarketSummaryResponse findSummary(String rawTicker) {
        String ticker = rawTicker.trim().toUpperCase(Locale.ROOT);
        if (!instrumentRepository.existsById(ticker)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found");
        }

        OffsetDateTime from = LocalDate.now(EXCHANGE_ZONE).atStartOfDay(EXCHANGE_ZONE).toOffsetDateTime();
        OffsetDateTime to = from.plusDays(1);
        TradeDailyStatistics statistics = tradeRepository.findDailyStatistics(ticker, from, to);
        return new MarketSummaryResponse(
                ticker,
                tradeRepository.findFirstByInstrumentSeccodeOrderByExecutedAtDesc(ticker).map(Trade::getPrice).orElse(null),
                statistics.getLowPrice(),
                statistics.getHighPrice(),
                statistics.getVolume() == null ? 0 : statistics.getVolume(),
                statistics.getTradeCount() == null ? 0 : statistics.getTradeCount()
        );
    }

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
