package com.stability.coareport.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";

        // Generate 5 different hashes for the same password
        System.out.println("=== GENERATING HASHES FOR: " + password + " ===\n");

        for (int i = 1; i <= 5; i++) {
            String hashedPassword = encoder.encode(password);
            boolean matches = encoder.matches(password, hashedPassword);
            System.out.println("Hash #" + i + ": " + hashedPassword);
            System.out.println("Verification: " + matches);
            System.out.println();
        }

        // Test with various known hashes
        System.out.println("=== TESTING KNOWN HASHES ===\n");

        String[] knownHashes = {
            "$2a$10$N9qo8uLOickgx2ZMRZoMye4ImeZIJFiM6UtOJWOVGXABFdBZv6jwi",
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"
        };

        for (String hash : knownHashes) {
            System.out.println("Hash: " + hash);
            System.out.println("Matches '" + password + "': " + encoder.matches(password, hash));
            System.out.println("Matches 'password': " + encoder.matches("password", hash));
            System.out.println("Matches 'admin123': " + encoder.matches("admin123", hash));
            System.out.println();
        }
    }
}
