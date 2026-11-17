package ru.trading.system.trade;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.trade.dto.MarketSummaryResponse;
import ru.trading.system.trade.dto.TradeHistoryResponse;

@Validated
@RestController
@RequestMapping("/api/v1/instruments")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService tradeService;

    @GetMapping("/{ticker}/summary")
    public MarketSummaryResponse findSummary( String ticker) {
        return tradeService.findSummary(ticker);
    }

    @GetMapping("/{ticker}/trades")
    public TradeHistoryResponse findHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return tradeService.findForInstrument(ticker, page, size);
    }
}
