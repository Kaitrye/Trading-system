package ru.trading.system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsBusinessReasonToStableErrorCode() {
    ApiError error =
        handler
            .handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient available balance"))
            .getBody();

    assertEquals(409, error.status());
    assertEquals("INSUFFICIENT_BALANCE", error.code());
    assertEquals("Insufficient available balance", error.message());
  }
}
