package com.stability.coareport.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcyrptExample {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String plainText = "Welcome@123";

        // Hash (ciphertext)
        String cipherText = encoder.encode(plainText);

        System.out.println("Plain Text  : " + plainText);
        System.out.println("Cipher Text : " + cipherText);

        // Verification
        boolean matches = encoder.matches(plainText, cipherText);
        System.out.println("Password Match: " + matches);
    }
}
