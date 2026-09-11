package com.ciphervault.ciphervault.test;

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

        System.out.println(
                "[INFO] Protected endpoint accessed."
        );

        String email = authentication.getName();

        System.out.println(
                "[SUCCESS] Authenticated user: "
                        + email
        );

        return "Protected endpoint accessed successfully. User: "
                + email;
    }
}




