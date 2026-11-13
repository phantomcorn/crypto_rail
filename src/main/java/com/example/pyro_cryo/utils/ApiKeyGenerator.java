package com.example.pyro_cryo.utils;

import io.jsonwebtoken.io.Encoders;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ApiKeyGenerator {

    private static final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        byte[] key = new byte[length];
        random.nextBytes(key);

        return Encoders.BASE64.encode(key);
    }

}
