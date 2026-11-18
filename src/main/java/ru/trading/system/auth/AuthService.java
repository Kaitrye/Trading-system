package ru.trading.system.auth;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.AccountService;
import ru.trading.system.user.Role;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;
import ru.trading.system.user.dto.LoginRequest;
import ru.trading.system.user.dto.RegisterRequest;
import ru.trading.system.user.dto.TokenResponse;
import ru.trading.system.user.dto.UserResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final AccountService accountService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());

    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    }

    User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
    User savedUser = userRepository.saveAndFlush(user);
    accountService.createDefaultAccount(savedUser);

    return new UserResponse(
        savedUser.getId(), savedUser.getEmail(), savedUser.getRole(), savedUser.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(normalizeEmail(request.email()))
            .orElseThrow(this::invalidCredentials);

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw invalidCredentials();
    }

    return new TokenResponse(
        jwtService.createToken(user), "Bearer", jwtService.expirationSeconds());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private ResponseStatusException invalidCredentials() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
  }
}
