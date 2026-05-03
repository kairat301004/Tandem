package org.example.tandem.controller;

import org.example.tandem.dto.user.*;
import org.example.tandem.entity.User;
import org.example.tandem.repository.UserRepository;
import org.example.tandem.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Получить всех пользователей (для списка в чате и задачах)
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    // Поиск пользователей
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String q) {
        List<User> users = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, q);
        List<UserDto> userDtos = users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    // Получить профиль текущего пользователя
    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> getProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        return ResponseEntity.ok(mapToProfileDto(user));
    }

    // Обновить профиль
    @PutMapping("/profile")
    public ResponseEntity<ProfileDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPosition(request.getPosition());
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);
        return ResponseEntity.ok(mapToProfileDto(saved));
    }

    // Сменить пароль
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    // Загрузить аватар
    @PostMapping("/avatar")
    public ResponseEntity<ProfileDto> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Создаём директорию для аватаров
        String uploadDir = System.getProperty("user.dir") + "/uploads/avatars/";
        Files.createDirectories(Paths.get(uploadDir));

        // Генерируем уникальное имя
        String fileName = user.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        file.transferTo(filePath.toFile());

        // Сохраняем URL аватара
        String avatarUrl = "/uploads/avatars/" + fileName;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return ResponseEntity.ok(mapToProfileDto(user));
    }

    private UserDto mapToUserDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPosition()
        );
    }

    private ProfileDto mapToProfileDto(User user) {
        ProfileDto.DepartmentInfo departmentInfo = null;
        if (user.getDepartment() != null) {
            departmentInfo = ProfileDto.DepartmentInfo.builder()
                    .id(user.getDepartment().getId())
                    .name(user.getDepartment().getName())
                    .build();
        }

        return ProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .position(user.getPosition())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .department(departmentInfo)
                .createdAt(user.getCreatedAt())
                .build();
    }
}