package com.example.crypto_rail.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // 2 minutes
    public static final long ACCESS_TOKEN_TTL = 1000L * 60L * 2L;
    // 6 hours
    public static final long REFRESH_TOKEN_TTL = 1000L * 60L * 60L * 6L;
    private final String secretKey;

    @Autowired
    Clock clock;

    public JwtService() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HMacSHA256");
            SecretKey secretKey = keyGen.generateKey();
            this.secretKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiredAt = extractClaim(token, Claims::getExpiration);
            return expiredAt.before(new Date(clock.millis()));
        } catch (ExpiredJwtException | SignatureException e) {
            return true;
        }
    }

    public String createToken(String username, long duration) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(clock.millis()))
                .expiration(new Date(clock.millis() + duration))
                .signWith(getKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .clock(() -> Date.from(clock.instant()))
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
