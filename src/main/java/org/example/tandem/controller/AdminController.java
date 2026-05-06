package org.example.tandem.controller;

import org.example.tandem.dto.DepartmentDto;
import org.example.tandem.dto.RoleDto;
import org.example.tandem.dto.admin.AdminUserResponse;
import org.example.tandem.dto.admin.CreateUserRequest;
import org.example.tandem.dto.admin.UpdateUserRequest;
import org.example.tandem.entity.Department;
import org.example.tandem.entity.Role;
import org.example.tandem.entity.User;
import org.example.tandem.repository.DepartmentRepository;
import org.example.tandem.repository.RoleRepository;
import org.example.tandem.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('MANAGE_USERS')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ ==========

    // Получить всех пользователей (с пагинацией)
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage;

        if (search != null && !search.isEmpty()) {
            usersPage = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        return ResponseEntity.ok(usersPage.map(this::mapToAdminResponse));
    }

    // Получить пользователя по ID
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable UUID id) {
        User user = userRepository.findByIdWithRolesAndDepartment(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(mapToAdminResponse(user));
    }

    // Создать пользователя
    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(@RequestBody CreateUserRequest request) {
        // Проверка на дубликат email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // 🔥 Преобразуем названия ролей в UUID
        Set<Role> roles = new HashSet<>();
        if (request.getRoleNames() != null && !request.getRoleNames().isEmpty()) {
            roles = roleRepository.findByNameIn(request.getRoleNames());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .position(request.getPosition())
                .phone(request.getPhone())
                .department(department)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        return new ResponseEntity<>(mapToAdminResponse(saved), HttpStatus.CREATED);
    }

    // Обновить пользователя
    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        User user = userRepository.findByIdWithRolesAndDepartment(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPosition(request.getPosition());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(department);
        }

        if (request.getRoleNames() != null) {
            Set<Role> roles = roleRepository.findByNameIn(request.getRoleNames());
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(mapToAdminResponse(saved));
    }

    // Удалить пользователя
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    // Сбросить пароль пользователя
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @RequestParam String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    // ========== УПРАВЛЕНИЕ РОЛЯМИ ==========

    @GetMapping("/roles")
    public ResponseEntity<Set<RoleDto>> getAllRoles() {
        Set<Role> roles = roleRepository.findAllWithPermissions();
        Set<RoleDto> roleDtos = roles.stream()
                .map(role -> new RoleDto(role.getId(), role.getName(), role.getDescription()))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roleDtos);
    }

    // ========== УПРАВЛЕНИЕ ДЕПАРТАМЕНТАМИ ==========

    @GetMapping("/departments")
    public ResponseEntity<Set<DepartmentDto>> getAllDepartments() {
        Set<Department> departments = departmentRepository.findAllWithManagers();
        Set<DepartmentDto> departmentDtos = departments.stream()
                .map(dept -> new DepartmentDto(dept.getId(), dept.getName(), dept.getDescription()))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(departmentDtos);
    }

    // Создать департамент
    @PostMapping("/departments")
    public ResponseEntity<DepartmentDto> createDepartment(@RequestParam String name, @RequestParam String description) {
        Department department = Department.builder()
                .name(name)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Department saved = departmentRepository.save(department);
        return new ResponseEntity<>(new DepartmentDto(saved.getId(), saved.getName(), saved.getDescription()), HttpStatus.CREATED);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private AdminUserResponse mapToAdminResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .position(user.getPosition())
                .phone(user.getPhone())
                .status(user.getStatus())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .roleNames(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}