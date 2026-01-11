package com.stability.coareport.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/password-check")
    public Map<String, Object> testPasswordEncoding(@RequestParam String password, @RequestParam String hash) {
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", passwordEncoder.matches(password, hash));
        result.put("newHash", passwordEncoder.encode(password));
        return result;
    }
}
