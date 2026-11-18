package ru.trading.system.position;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.position.dto.PositionResponse;

@RestController
@RequestMapping("/api/v1/positions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PositionController {

  private final PositionService positionService;

  @GetMapping
  public List<PositionResponse> findMyPositions(@AuthenticationPrincipal UUID userId) {
    return positionService.findForUser(userId);
  }
}
