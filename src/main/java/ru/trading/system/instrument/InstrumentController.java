package ru.trading.system.instrument;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.instrument.dto.InstrumentResponse;

@RestController
@RequestMapping("/api/v1/instruments")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class InstrumentController {

  private final InstrumentService instrumentService;

  @GetMapping
  public List<InstrumentResponse> findAll() {
    return instrumentService.findAll();
  }

  @GetMapping("/{ticker}")
  public InstrumentResponse findByTicker(@PathVariable String ticker) {
    return instrumentService.findByTicker(ticker);
  }
}
