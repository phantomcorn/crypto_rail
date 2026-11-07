package com.example.crypto_rail.utils;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Map;

public class HMacUtil {

    public static String sign(String key, Map<String, Object> jsonPayload) {

        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));

        return Jwts.builder()
                .claims(jsonPayload)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public static boolean verify(String signature, String key) {

        SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));

        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(signature);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }
}
