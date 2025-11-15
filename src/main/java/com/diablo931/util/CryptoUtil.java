package com.diablo931.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
    private static final String KEY = "1234567890123456"; // 16 bytes

    public static String encrypt(String input) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes()));
        } catch (Exception e) {
            return "";
        }
    }

    public static String decrypt(String encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encoded)));
        } catch (Exception e) {
            return "";
        }
    }
}
