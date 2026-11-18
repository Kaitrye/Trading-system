package ru.trading.system.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trading.system.config.JwtProperties;
import ru.trading.system.user.User;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtProperties jwtProperties;

  public String createToken(User user) {
    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plusSeconds(jwtProperties.expirationSeconds());

    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("role", user.getRole().name())
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey())
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
  }

  public long expirationSeconds() {
    return jwtProperties.expirationSeconds();
  }

  private SecretKey signingKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
  }
}
