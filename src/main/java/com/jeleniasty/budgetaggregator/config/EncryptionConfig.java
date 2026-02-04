package com.jeleniasty.budgetaggregator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class EncryptionConfig {

    @Bean
    public SecretKey aesKey(@Value("${security.encryption.aes.key}") String aesKey) {
        byte[] decodedKey = Base64.getDecoder().decode(aesKey);

        if (decodedKey.length != 32) {
            throw new IllegalStateException(
                    "AES-256 key must be exactly 32 bytes"
            );
        }

        return new SecretKeySpec(decodedKey, "AES");
    }

    @Bean
    public String hmacKey(@Value("${security.encryption.hmac.key}") String hmacKey) {
        byte[] decodedKey = Base64.getDecoder().decode(hmacKey);

        if (decodedKey.length < 32) {
            throw new IllegalStateException(
                    "HMAC key should be at least 32 bytes"
            );
        }

        return hmacKey;
    }
}
