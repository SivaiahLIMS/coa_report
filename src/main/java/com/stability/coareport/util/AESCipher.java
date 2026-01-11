package com.stability.coareport.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESCipher {

    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "MySuperSecretKey".getBytes(); // 16 bytes

    // Encrypt plaintext
    public static String encrypt(String data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Decrypt ciphertext
    public static String decrypt(String encryptedData) throws Exception {
        SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedBytes));
    }

    public static void main(String[] args) throws Exception {
        String plainText = "Welcome@123";

        String cipherText = encrypt(plainText);
        String decryptedText = decrypt(cipherText);

        System.out.println("Plain Text  : " + plainText);
        System.out.println("Cipher Text : " + cipherText);
        System.out.println("Decrypted   : " + decryptedText);
    }
}

