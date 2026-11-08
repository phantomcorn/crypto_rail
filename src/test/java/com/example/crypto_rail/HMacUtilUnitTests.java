package com.example.crypto_rail;

import com.example.crypto_rail.utils.HMacUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;

@ActiveProfiles("test")
@Transactional
public class HMacUtilUnitTests {

    SecretKey secretKey;
    String key;

    @BeforeEach
    public void generateKey() {
        secretKey = Jwts.SIG.HS256.key().build();
        key = Encoders.BASE64.encode(secretKey.getEncoded());
    }

    @Test
    public void canSignKey() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.put("message1", "Hello");
        jsonObj.put("message2", "World");

       String signature = HMacUtil.sign(key, jsonObj.getMap());
       Assertions.assertNotNull(signature);
    }

    @Test
    public void verifySignatureWithSameMessageReturnsTrue() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.put("message1", "Hello");
        jsonObj.put("message2", "World");

        String signature = HMacUtil.sign(key, jsonObj.getMap());
        Assertions.assertTrue(HMacUtil.verify(signature, key));
    }

    @Test
    public void verifyTamperedSignatureWillReturnFalse() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.put("message1", "Hello");
        jsonObj.put("message2", "World");

        String signature = HMacUtil.sign(key, jsonObj.getMap());
        String tamperedSignature = signature + "2";

        Assertions.assertFalse(HMacUtil.verify(tamperedSignature, key));
    }



}
