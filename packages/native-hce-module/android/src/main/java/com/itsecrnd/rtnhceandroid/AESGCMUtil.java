package com.itsecrnd.rtnhceandroid;

import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@SuppressLint("NewApi")
public class AESGCMUtil {
    public static SecretKey generateKey() {
        KeyGenerator keyGen;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to get AES algorithm.", e);
        }
        keyGen.init(256);
        return keyGen.generateKey();
    }

    public static byte[] generateNonce(int size) {
        byte[] nonce = new byte[size];
        new SecureRandom().nextBytes(nonce); // Fill nonce with random bytes
        return nonce;
    }

    public static String encryptData(SecretKey key, String plaintext) {
        try {
            byte[] iv = new byte[32];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertextBytes = cipher.doFinal(plaintextBytes);

            return Base64.getEncoder().encodeToString(iv) + ';' +
                    Base64.getEncoder().encodeToString(ciphertextBytes);
        } catch (NoSuchPaddingException
                 | NoSuchAlgorithmException
                 | IllegalBlockSizeException
                 | InvalidAlgorithmParameterException
                 | BadPaddingException
                 | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptData(SecretKey key, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            String[] parts = ciphertext.split(";");

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid ciphertext parameter.");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertextBytes = Base64.getDecoder().decode(parts[1]);

            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            byte[] plaintextBytes = cipher.doFinal(ciphertextBytes);
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException
                 | IllegalBlockSizeException
                 | InvalidAlgorithmParameterException
                 | BadPaddingException
                 | InvalidKeyException
                 | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
