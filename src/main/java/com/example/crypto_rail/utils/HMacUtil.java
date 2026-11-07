package com.example.crypto_rail.utils;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class HMacUtil {

    public static String sign(String keyString, String jsonPayload) {

        SecretKey key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .header().contentType("application/json")
                .and()
                .content(jsonPayload)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public static boolean verify(String signature, String keyString) {

        SecretKey key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));

        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(signature);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
