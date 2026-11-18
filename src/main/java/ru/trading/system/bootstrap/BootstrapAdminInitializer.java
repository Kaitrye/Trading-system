package ru.trading.system.bootstrap;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);
  private final BootstrapAdminService bootstrapAdminService;

  @Override
  public void run(ApplicationArguments args) {
    if (bootstrapAdminService.createIfConfigured()) {
      log.info("Bootstrap administrator created");
    }
  }
}
