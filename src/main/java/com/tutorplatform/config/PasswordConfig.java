package com.tutorplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PASSWORD ENCODER CONFIGURATION
 * =====================================================
 * WHY A SEPARATE CLASS?
 * We had a circular dependency:
 *   SecurityConfig defines PasswordEncoder @Bean
 *   SecurityConfig also @Autowires UserService
 *   UserService @Autowires PasswordEncoder
 *   → Spring gets confused: which comes first?
 *
 * FIX: Move PasswordEncoder to its OWN @Configuration class
 * so it has no dependencies and can be created first.
 * Now the order is:
 *   1. PasswordConfig creates PasswordEncoder
 *   2. UserService gets PasswordEncoder injected
 *   3. SecurityConfig gets UserService injected
 *   No circles!
 * =====================================================
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt password encoder bean.
     * BCrypt automatically generates a salt and hashes the password.
     * The same plain-text password will produce different hashes each time,
     * but BCrypt can still verify them (it embeds the salt in the hash).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
