package ru.trading.system.order;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.order.dto.OrderResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(userId, request);
    }

    @DeleteMapping("/{orderId}")
    public OrderResponse cancel(@AuthenticationPrincipal UUID userId, @PathVariable UUID orderId) {
        return orderService.cancel(userId, orderId);
    }
}
