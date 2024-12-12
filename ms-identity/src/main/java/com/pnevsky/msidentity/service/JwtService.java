package com.pnevsky.msidentity.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public void validateToken(String token) {
        Jwts.parser().verifyWith(key).requireIssuer("identity-service")
                .build().parseSignedClaims(token);
    }

    public String generateToken(String username) {
        Date now = new Date();
        return Jwts.builder().issuer("identity-service").subject(username).issuedAt(now)
                .expiration(new Date(now.getTime() + Duration.ofMinutes(30).toMillis()))
                .signWith(key).compact();
    }
}
