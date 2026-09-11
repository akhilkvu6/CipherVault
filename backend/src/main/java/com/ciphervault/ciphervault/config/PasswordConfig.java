package com.ciphervault.ciphervault.config;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        ConsoleLogger.info("Initializing BCrypt password encoder...");

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        ConsoleLogger.success("BCrypt password encoder initialized successfully.");

        return passwordEncoder;
    }
}