package org.example.tandem.controller;

import jakarta.servlet.http.HttpSession;
import org.example.tandem.dto.auth.AuthResponse;
import org.example.tandem.dto.auth.LoginRequest;
import org.example.tandem.entity.User;
import org.example.tandem.security.CustomUserDetails;
import org.example.tandem.service.auth.AuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Transactional
    public AuthResponse login(@RequestBody LoginRequest request, HttpSession session) {
        // 1. Получаем пользователя с ролями из БД (ОДИН ЗАПРОС!)
        User user = authService.findUserByEmail(request.getEmail());

        // 2. Проверяем пароль
        authService.validatePassword(request.getPassword(), user.getPasswordHash());

        // 3. Создаем AuthResponse
        AuthResponse authResponse = authService.toAuthResponse(user);

        // 4. Создаем объект аутентификации Spring Security
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 5. Устанавливаем аутентификацию в SecurityContext
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(auth);

        // 6. Сохраняем SecurityContext в сессию
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute("userId", authResponse.getId());

        return authResponse;
    }
}