package com.stability.coareport.controller;

import com.stability.coareport.dto.PageResponse;
import com.stability.coareport.dto.PasswordUpdateRequest;
import com.stability.coareport.dto.UserRegistrationRequest;
import com.stability.coareport.entity.Branch;
import com.stability.coareport.entity.User;
import com.stability.coareport.repository.BranchRepository;
import com.stability.coareport.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        try {
            if (userRepository.findByUsername(request.getEmpId()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Employee ID already exists"));
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));

            User user = new User();
            user.setUsername(request.getEmpId());
            user.setEmpId(request.getEmpId());
            user.setFirstName(request.getFirstName());
            user.setMiddleName(request.getMiddleName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setMobileNumber(request.getMobileNumber());
            user.setDepartment(request.getDepartment());
            user.setPassword(passwordEncoder.encode("Welcome@123"));
            user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            user.setBranch(branch);
            user.setActive(true);

            User savedUser = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedUser.getId());
            response.put("empId", savedUser.getEmpId());
            response.put("email", savedUser.getEmail());
            response.put("firstName", savedUser.getFirstName());
            response.put("middleName", savedUser.getMiddleName());
            response.put("lastName", savedUser.getLastName());
            response.put("department", savedUser.getDepartment());
            response.put("role", savedUser.getRole());
            response.put("message", "User registered successfully with default password: Welcome@123");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<PageResponse<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        } else {
            pageRequest = PageRequest.of(page, size, Sort.by(direction, "id"));
        }

        Page<User> userPage = userRepository.findAll(pageRequest);
        return ResponseEntity.ok(PageResponse.fromSpringPage(userPage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody PasswordUpdateRequest request, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "New passwords do not match"));
            }

            if (request.getNewPassword().length() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters long"));
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRegistrationRequest request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getMiddleName() != null) user.setMiddleName(request.getMiddleName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
            if (request.getEmail() != null) user.setEmail(request.getEmail());
            if (request.getMobileNumber() != null) user.setMobileNumber(request.getMobileNumber());
            if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
            if (request.getRole() != null) user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));

            User updatedUser = userRepository.save(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setActive(false);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean newStatus = request.getOrDefault("active", !user.getActive());
            user.setActive(newStatus);
            userRepository.save(user);

            String message = newStatus ? "User activated successfully" : "User deactivated successfully";
            return ResponseEntity.ok(Map.of("message", message, "active", newStatus));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
