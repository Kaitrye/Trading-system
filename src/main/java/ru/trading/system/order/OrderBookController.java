package ru.trading.system.order;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.order.dto.OrderBookResponse;

@RestController
@RequestMapping("/api/v1/instruments")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderBookController {
    private final OrderBookService orderBookService;

    @GetMapping("/{ticker}/order-book")
    public OrderBookResponse getOrderBook(@PathVariable String ticker) {
        return orderBookService.getOrderBook(ticker);
    }
}
