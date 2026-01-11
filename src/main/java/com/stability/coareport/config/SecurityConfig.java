package com.stability.coareport.config;

import com.stability.coareport.security.JwtAuthenticationFilter;
import com.stability.coareport.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider() {
            @Override
            protected void additionalAuthenticationChecks(org.springframework.security.core.userdetails.UserDetails userDetails,
                                                          org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication) {
                String presentedPassword = authentication.getCredentials().toString();
                String encodedPassword = userDetails.getPassword();

                System.out.println("=== PASSWORD COMPARISON ===");
                System.out.println("Presented password: [" + presentedPassword + "]");
                System.out.println("Presented length: " + presentedPassword.length());
                System.out.println("Encoded password: " + encodedPassword);
                System.out.println("Encoded length: " + encodedPassword.length());
                System.out.println("Matches: " + passwordEncoder().matches(presentedPassword, encodedPassword));

                super.additionalAuthenticationChecks(userDetails, authentication);
            }
        };
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/reports/pdf/view/**").permitAll()
                        .requestMatchers("/api/reports/pdf/download/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService) {
//            @Override
//            protected void additionalAuthenticationChecks(org.springframework.security.core.userdetails.UserDetails userDetails,
//                                                          org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication) {
//                String presentedPassword = authentication.getCredentials().toString();
//                String encodedPassword = userDetails.getPassword();
//
//                System.out.println("=== PASSWORD COMPARISON ===");
//                System.out.println("Presented password: [" + presentedPassword + "]");
//                System.out.println("Presented length: " + presentedPassword.length());
//                System.out.println("Encoded password: " + encodedPassword);
//                System.out.println("Encoded length: " + encodedPassword.length());
//                System.out.println("Matches: " + passwordEncoder().matches(presentedPassword, encodedPassword));
//
//                super.additionalAuthenticationChecks(userDetails, authentication);
//            }
//        };
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }
}
