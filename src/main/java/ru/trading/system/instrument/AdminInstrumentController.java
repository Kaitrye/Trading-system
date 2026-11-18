package ru.trading.system.instrument;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.trading.system.instrument.dto.CreateInstrumentRequest;
import ru.trading.system.instrument.dto.InstrumentResponse;
import ru.trading.system.instrument.dto.UpdateInstrumentStatusRequest;

@RestController
@RequestMapping("/api/v1/admin/instruments")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminInstrumentController {
  private final InstrumentService instrumentService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InstrumentResponse create(@Valid @RequestBody CreateInstrumentRequest request) {
    return instrumentService.create(request);
  }

  @PatchMapping("/{ticker}/status")
  public InstrumentResponse changeStatus(
      @PathVariable String ticker, @RequestBody UpdateInstrumentStatusRequest request) {
    return instrumentService.changeStatus(ticker, request.active());
  }
}
