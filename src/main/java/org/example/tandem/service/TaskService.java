package org.example.tandem.service;

import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.dto.task.TaskRequest;
import org.example.tandem.dto.task.TaskResponse;
import org.example.tandem.entity.Task;
import org.example.tandem.entity.User;
import org.example.tandem.enums.TaskPriority;
import org.example.tandem.enums.TaskStatus;
import org.example.tandem.repository.TaskRepository;
import org.example.tandem.repository.UserRepository;
import org.example.tandem.security.CustomUserDetails;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, FileService fileService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Пользователь не авторизован");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    //CREATE
    @Transactional
    @CacheEvict(value = {"allTasks", "userTasks"}, allEntries = true)
    public TaskResponse createTask(TaskRequest request, List<MultipartFile> files) {
        User currentUser = getCurrentUser();

        User assigned = null;
        if (request.getAssignedId() != null) {
            assigned = userRepository.findById(request.getAssignedId())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + request.getAssignedId()));
        }
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus().name())
                .priority(request.getPriority().name())
                .deadline(request.getDeadline())
                .creator(currentUser)
                .assigned(assigned)
                .createdAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();

        Task savedTask = taskRepository.save(task);

        // Добавляем файлы, если они есть
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                fileService.uploadFileForTask(file, savedTask);
            }
        }
        return mapToResponse(savedTask);
    }

    // READ ALL (с пагинацией)
    @Cacheable(value = "allTasks", key = "#page + '_' + #size")
    public Page<TaskResponse> getAllTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Task> taskPage = taskRepository.findAllByOrderByCreatedAtDesc(pageable);
        return taskPage.map(this::mapToResponse);
    }

    // READ BY ID
    @Cacheable(value = "task", key = "#id")
    public TaskResponse getTaskById(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Задача не найдена: " + id));
        return mapToResponse(task);
    }

    // READ MY TASKS (где я создатель или исполнитель)
    @Cacheable(value = "userTasks", key = "#userId + '_' + #page + '_' + #size")
    public Page<TaskResponse> getMyTasks (UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Task> taskPage = taskRepository.findByUserId(userId, pageable);
        return taskPage.map(this::mapToResponse);
    }

    // UPDATE
    @Transactional
    @CacheEvict(value = {"task", "allTasks", "userTasks"}, allEntries = true)
    public TaskResponse updateTask(UUID id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Задача не найдена: " + id));

        User currentUser = getCurrentUser();

        // Проверка прав: создатель или админ
        boolean isCreator = task.getCreator().getId().equals(currentUser.getId());
        boolean hasPermission = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("UPDATE_TASK"));

        if (!isCreator && !hasPermission) {
            throw new AccessDeniedException("Нет прав для обновления этой задачи");
        }

        // Обновляем поля
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus().name());
        task.setPriority(request.getPriority().name());
        task.setDeadline(request.getDeadline());
        task.setUpdateAt(LocalDateTime.now());

        if (request.getAssignedId() != null) {
            User assigned = userRepository.findById(request.getAssignedId())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + request.getAssignedId()));
            task.setAssigned(assigned);
        } else {
            task.setAssigned(null);
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    // UPDATE STATUS (быстрое изменение статуса)
    @Transactional
    @CacheEvict(value = {"task", "allTasks", "userTasks"}, allEntries = true)
    public TaskResponse updateTaskStatus(UUID id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Задача не найдена: " + id));

        User currentUser = getCurrentUser();

        // Статус могут менять: создатель, исполнитель или админ
        boolean isCreator = task.getCreator().getId().equals(currentUser.getId());
        boolean isAssigned = task.getAssigned() != null && task.getAssigned().getId().equals(currentUser.getId());
        boolean hasPermission = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("UPDATE_TASK"));

        if (!isCreator && !isAssigned && !hasPermission) {
            throw new AccessDeniedException("Нет прав для изменения статуса задачи");
        }

        task.setStatus(status.name());
        task.setUpdateAt(LocalDateTime.now());

        Task updatedtask = taskRepository.save(task);
        return mapToResponse(updatedtask);
    }

    // DELETE
    @Transactional
    @CacheEvict(value = {"task", "allTasks", "userTasks"}, allEntries = true)
    public void deleteTask(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Задача не найдена: " + id));

        User currentUser = getCurrentUser();

        boolean isCreator = task.getCreator().getId().equals(currentUser.getId());
        boolean hasPermission = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("DELETE_TASK"));

        if (!isCreator && !hasPermission) {
            throw new AccessDeniedException("Нет прав для удаления этой задачи");
        }

        // Удаляем связанные файлы
        fileService.deleteFilesByTaskId(id);

        taskRepository.delete(task);
    }

    // Проверка, яаляется ли пользователь создателем задачи (для @PreAuthorize)
    public boolean isCreator(UUID taskId, UUID userId) {
        return taskRepository.existsByIdAndCreatorId(taskId, userId);
    }

    // Вспомогательный метод для маппинга
    private TaskResponse mapToResponse(Task task) {
        List<FileResponse> files = fileService.getFilesByTaskId(task.getId());

        String assignedName = null;
        if (task.getAssigned() != null) {
            assignedName = task.getAssigned().getFirstName() + " " + task.getAssigned().getLastName();
        }

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                TaskStatus.valueOf(task.getStatus()),
                TaskPriority.valueOf(task.getPriority()),
                task.getDeadline(),
                task.getCreatedAt(),
                task.getUpdateAt(),
                task.getCreator().getFirstName() + " " + task.getCreator().getLastName(),
                assignedName,
                files
        );
    }
}
