package com.ciphervault.ciphervault.test;

import com.ciphervault.ciphervault.util.ConsoleLogger;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public String protectedEndpoint(
            Authentication authentication) {

        ConsoleLogger.info(
                "Protected endpoint accessed."
        );

        String email = authentication.getName();

        ConsoleLogger.success(
                "Authenticated user: " + email
        );

        return "Protected endpoint accessed successfully. User: "
                + email;
    }
}