//package org.example.tandem.service.auth;
//
//import org.example.tandem.dto.auth.AuthResponse;
//import org.example.tandem.dto.auth.LoginRequest;
//import org.example.tandem.entity.User;
//import org.example.tandem.mapping.UserMapper;
//import org.example.tandem.repository.UserRepository;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder encoder;
//    private final UserMapper userMapper;
//
//    public AuthService(UserRepository userRepository, PasswordEncoder encoder, UserMapper userMapper) {
//        this.userRepository = userRepository;
//        this.encoder = encoder;
//        this.userMapper = userMapper;
//    }
//
//    // ЕДИНСТВЕННЫЙ метод для получения пользователя с ролями
//    public User findUserByEmail(String email) {
//        return userRepository.findByEmailWithRoles(email)
//                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
//    }
//
//    // Метод для проверки пароля
//    public void validatePassword(String rawPassword, String encodedPassword) {
//        if (!encoder.matches(rawPassword, encodedPassword)) {
//            throw new BadCredentialsException("Invalid email or password");
//        }
//    }
//
//    // Метод для преобразования в AuthResponse
//    public AuthResponse toAuthResponse(User user) {
//        return userMapper.toAuthResponse(user);
//    }
//
//}

package org.example.tandem.service.auth;

import org.example.tandem.dto.auth.AuthResponse;
import org.example.tandem.entity.User;
import org.example.tandem.mapping.UserMapper;
import org.example.tandem.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder encoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.userMapper = userMapper;
    }

    /**
     * Получение пользователя по email с загрузкой ролей и прав
     */
    public User findUserByEmail(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    }

    /**
     * Получение пользователя по ID
     */
    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    /**
     * Проверка пароля
     */
    public void validatePassword(String rawPassword, String encodedPassword) {
        if (!encoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Преобразование User → AuthResponse
     */
    public AuthResponse toAuthResponse(User user) {
        return userMapper.toAuthResponse(user);
    }
}