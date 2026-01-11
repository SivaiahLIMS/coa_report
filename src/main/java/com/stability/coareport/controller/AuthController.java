package com.stability.coareport.controller;

import com.stability.coareport.dto.JwtResponse;
import com.stability.coareport.dto.LoginRequest;
import com.stability.coareport.entity.User;
import com.stability.coareport.repository.UserRepository;
import com.stability.coareport.security.JwtUtil;
import com.stability.coareport.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Username: " + loginRequest.getUsername());
            System.out.println("Password length: " + (loginRequest.getPassword() != null ? loginRequest.getPassword().length() : 0));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            System.out.println("Authentication successful!");

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            JwtResponse response = new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    user.getRole().name(),
                    user.getBranch().getId(),
                    user.getBranch().getName(),
                    user.getBranch().getCompany().getId(),
                    user.getBranch().getCompany().getName()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("=== LOGIN FAILED ===");
            System.out.println("Error: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
