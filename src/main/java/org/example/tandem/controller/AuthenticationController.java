package org.example.tandem.controller;

import org.example.tandem.dto.auth.LoginRequest;
import org.example.tandem.entity.User;
import org.example.tandem.service.auth.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public User login(@RequestBody LoginRequest request) {
        return authService.login(request); // создаст сессию
    }
}