package ru.trading.system.corporate;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorporateEventScheduler {
  private static final ZoneId EXCHANGE_ZONE = ZoneId.of("Europe/Moscow");

  private final CorporateEventService corporateEventService;

  @Scheduled(cron = "${app.corporate-events.cron:0 5 0 * * *}")
  public void processDailyEvents() {
    corporateEventService.processEvents(LocalDate.now(EXCHANGE_ZONE));
  }
}
