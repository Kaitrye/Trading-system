package ru.trading.system.order;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.order.dto.CreateOrderRequest;
import ru.trading.system.order.dto.OrderResponse;
import ru.trading.system.order.dto.UpdateOrderRequest;

@RestController
@RequestMapping("/api/v1/orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  @GetMapping
  public List<OrderResponse> findMyOrders(@AuthenticationPrincipal UUID userId) {
    return orderService.findForUser(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(
      @AuthenticationPrincipal UUID userId, @Valid @RequestBody CreateOrderRequest request) {
    return orderService.create(userId, request);
  }

  @PatchMapping("/{orderId}")
  public OrderResponse update(
      @AuthenticationPrincipal UUID userId,
      @PathVariable UUID orderId,
      @Valid @RequestBody UpdateOrderRequest request) {
    return orderService.update(userId, orderId, request);
  }

  @DeleteMapping("/{orderId}")
  public OrderResponse cancel(@AuthenticationPrincipal UUID userId, @PathVariable UUID orderId) {
    return orderService.cancel(userId, orderId);
  }
}
