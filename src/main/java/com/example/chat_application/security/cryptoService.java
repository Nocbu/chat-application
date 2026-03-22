package com.example.chat_application.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class cryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LEN_BYTES = 12;
    private static final int TAG_LEN_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public cryptoService(@Value("${app.crypto.aesKeyBase64}") String aesKeyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes after Base64 decoding.");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    //for iv and cipher tag
    public String encryptToString(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decryptToString(String encrypted) {
        if (encrypted == null) return null;
        try {
            String[] parts = encrypted.split(":", 2);
            if (parts.length != 2) {
                //return old one
                return encrypted;
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] plain = cipher.doFinal(cipherBytes);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}