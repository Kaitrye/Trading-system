package ru.trading.system.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import ru.trading.system.account.AccountService;
import ru.trading.system.user.User;
import ru.trading.system.user.UserRepository;
import ru.trading.system.user.dto.LoginRequest;
import ru.trading.system.user.dto.RegisterRequest;
import ru.trading.system.user.dto.TokenResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private AccountService accountService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  @InjectMocks private AuthService authService;

  @Test
  void registersNormalizedEmailAndCreatesDefaultAccount() {
    when(userRepository.existsByEmailIgnoreCase("user@example.test")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hash");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = authService.register(new RegisterRequest(" User@Example.Test ", "password123"));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(userCaptor.capture());
    verify(accountService).createDefaultAccount(userCaptor.getValue());
    assertEquals("user@example.test", userCaptor.getValue().getEmail());
    assertEquals("hash", userCaptor.getValue().getPasswordHash());
    assertEquals("user@example.test", response.email());
  }

  @Test
  void rejectsDuplicateEmailBeforeEncodingPassword() {
    when(userRepository.existsByEmailIgnoreCase("user@example.test")).thenReturn(true);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> authService.register(new RegisterRequest("USER@example.test", "password123")));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void createsBearerTokenForValidCredentials() {
    User user = new User("user@example.test", "hash", ru.trading.system.user.Role.USER);
    when(userRepository.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
    when(jwtService.createToken(user)).thenReturn("token");
    when(jwtService.expirationSeconds()).thenReturn(3600L);

    TokenResponse response =
        authService.login(new LoginRequest(" User@Example.Test ", "password123"));

    assertEquals("token", response.accessToken());
    assertEquals("Bearer", response.tokenType());
    assertEquals(3600L, response.expiresIn());
  }

  @Test
  void rejectsInvalidPasswordWithoutCreatingToken() {
    User user = new User("user@example.test", "hash", ru.trading.system.user.Role.USER);
    when(userRepository.findByEmailIgnoreCase("user@example.test")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new LoginRequest("user@example.test", "wrong-password")));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    verify(jwtService, never()).createToken(any());
  }
}
