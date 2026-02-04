package com.jeleniasty.budgetaggregator.service;

import com.jeleniasty.budgetaggregator.exception.EncryptionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class EncryptionService {
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey aesKey;
    private final String hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plaintext) {
        try {
            byte[] initializationVector = new byte[IV_LENGTH];
            secureRandom.nextBytes(initializationVector);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, initializationVector)
            );

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(UTF_8)
            );

            ByteBuffer buffer = ByteBuffer.allocate(initializationVector.length + ciphertext.length);
            buffer.put(initializationVector);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Unexpected exception during encryption. Message: " + e.getMessage(), e.getCause());
        }
    }

    public String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] initializationVector = new byte[IV_LENGTH];
            buffer.get(initializationVector);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    aesKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, initializationVector)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Unexpected exception during decryption. Message: " + e.getMessage(), e.getCause());
        }
    }

    public String generateBlindIndex(String plaintext) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKey.getBytes(UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);

            byte[] hash = sha256HMAC.doFinal(plaintext.getBytes(UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new EncryptionException("Failed to generate blind index", e);
        }
    }
}
