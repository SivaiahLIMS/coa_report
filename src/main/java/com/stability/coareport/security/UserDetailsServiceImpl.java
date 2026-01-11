package com.stability.coareport.security;

import com.stability.coareport.entity.User;
import com.stability.coareport.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("=== LOADING USER ===");
        System.out.println("Username: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        System.out.println("User found - ID: " + user.getId());
        System.out.println("Password hash: " + user.getPassword());
        System.out.println("Role: " + user.getRole());
        System.out.println("Branch: " + (user.getBranch() != null ? user.getBranch().getId() : "null"));

        UserDetails userDetails = UserDetailsImpl.build(user);
        System.out.println("UserDetails created successfully");

        return userDetails;
    }
}
