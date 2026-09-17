package com.example.eventflow.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties properties;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
  }

  public String issue(
      Long userId,
      String username,
      Long tenantId,
      String tenantType,
      String roleCode,
      int tokenVersion) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + properties.getExpireSeconds() * 1000);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("username", username)
        .claim("tenantId", tenantId)
        .claim("tenantType", tenantType)
        .claim("roleCode", roleCode)
        .claim("ver", tokenVersion)
        .issuedAt(now)
        .expiration(exp)
        .signWith(key())
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey key() {
    byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }
}
