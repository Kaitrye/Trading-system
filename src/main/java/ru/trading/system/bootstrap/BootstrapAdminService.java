package ru.trading.system.bootstrap;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trading.system.account.AccountService;
import ru.trading.system.config.BootstrapAdminProperties;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;

@Service
@RequiredArgsConstructor
public class BootstrapAdminService {
  private final BootstrapAdminProperties properties;
  private final UserRepository userRepository;
  private final AccountService accountService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public boolean createIfConfigured() {
    boolean noEmail = properties.email() == null || properties.email().isBlank();
    boolean noPassword = properties.password() == null || properties.password().isBlank();
    if (noEmail && noPassword) {
      return false;
    }
    if (noEmail || noPassword) {
      throw new IllegalStateException("Both bootstrap admin email and password must be configured");
    }
    if (properties.password().length() < 8) {
      throw new IllegalStateException(
          "Bootstrap admin password must be at least 8 characters long");
    }
    String email = properties.email().trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmailIgnoreCase(email)) {
      return false;
    }
    User admin =
        userRepository.saveAndFlush(
            new User(email, passwordEncoder.encode(properties.password()), Role.ADMIN));
    accountService.createDefaultAccount(admin);
    return true;
  }
}
