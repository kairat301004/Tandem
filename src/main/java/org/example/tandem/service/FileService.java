package org.example.tandem.service;

import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.entity.File;
import org.example.tandem.entity.News;
import org.example.tandem.entity.Task;
import org.example.tandem.entity.User;
import org.example.tandem.repository.FileRepository;
import org.example.tandem.repository.UserRepository;
import org.example.tandem.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class FileService {

    @Value("${file.upload-dir}")
    private String uploaderDir;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public FileService(FileRepository fileRepository, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Проверяем: есть ли авторизация и является ли Principal нашим классом
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("Пользователь не авторизован");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    // Метод для загрузки файла
    @Transactional
    public FileResponse uploadFile(MultipartFile multipartFile) {

        // 1. Получить текущего пользователя
        User currentUser = getCurrentUser();

        // 2. Проверить файл (не пустой)
        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        // 3. Создать уникальное имя файла
        String originalFileName = multipartFile.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);

        // 4. Сохранить файл на диск
//        try {
//            Path uploadPath = Paths.get(uploaderDir);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            Path filePath = uploadPath.resolve(uniqueFileName);
//            multipartFile.transferTo(filePath.toFile());
//        } catch (IOException e) {
//            throw new RuntimeException("Ошибка сохранения файла: " + e.getMessage(), e);
//        }
        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploaderDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(uniqueFileName);

            multipartFile.transferTo(filePath);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла", e);
        }

        // 5. Создать запись в БД
        File file = File.builder()
                .fileName(originalFileName)
                .fileUrl(uploaderDir + "/" + uniqueFileName)
                .fileType(multipartFile.getContentType())
                .size(multipartFile.getSize())
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        File savedFile = fileRepository.save(file);

        // 6. Вернуть FileResponse
        return mapToResponse(savedFile);

    }

    /**
     * Получить все файлы, прикрепленные к новости
     */
    public List<FileResponse> getFilesByNewsId(UUID newsId) {
        List<File> files = fileRepository.findByNewsId(newsId);
        return files.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Удалить все файлы, прикрепленные к новости
     */
    @Transactional
    public void deleteFilesByNewsId(UUID newsId) {
        List<File> files = fileRepository.findByNewsId(newsId);

        for (File file : files) {
            // Удаляем физический файл с диска
            try {
                Path filePath = Paths.get(file.getFileUrl());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Логируем ошибку, но не прерываем удаление остальных
                System.err.println("Failed to delete file: " + file.getFileUrl());
            }

            // Удаляем запись из БД
            fileRepository.delete(file);
        }
    }

    //Метод для сохранения файла с новостями
    @Transactional
    public void uploadFileForNews(MultipartFile multipartFile, News news) {

        User currentUser = getCurrentUser();

        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        String originalFileName = multipartFile.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);

        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploaderDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(uniqueFileName);

            multipartFile.transferTo(filePath);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла", e);
        }

        File file = File.builder()
                .fileName(originalFileName)
                .fileUrl(uploaderDir + "/" + uniqueFileName)
                .fileType(multipartFile.getContentType())
                .size(multipartFile.getSize())
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .news(news)
                .build();

        fileRepository.save(file);
    }

    //Метод для скачивания файла
    public Resource downloadFile(UUID fileId) {
        // 1. Найти файл в БД
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден с id: " + fileId));

        // 2. Получить путь к файлу
        try {
            Path filePath = Paths.get(file.getFileUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable()) {
                throw new RuntimeException("Файл не найден или не прочитан: " + file.getFileUrl());
            }

            // 3. Загрузить как Resource
            return resource;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    //Метод для получения списка файлов (с пагинацией)
    public Page<FileResponse> getALlFiles(int page, int size) {
        // 1. Создать Pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        // 2. Получить Page<File> из репозитория
        Page<File> filesPage = fileRepository.findAllByOrderByUploadedAtDesc(pageable);

        // 3. Преобразовать в Page<FileResponse>
        return filesPage.map(this::mapToResponse);
    }

    //Получить файлы конкретного пользователя
    public Page<FileResponse> getFilesByUser(UUID userId, int page, int size) {
        // Проверяем, существует ли пользователь
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Пользователь не найден с id: " + userId);
        }

        //Создаем пагинацию
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        //Получаем файлы пользователя
        Page<File> filesPage = fileRepository.findByUploaderIdOrderByUploadedAtDesc(userId, pageable);

        //Преобразуем в DTO
        return filesPage.map(this::mapToResponse);
    }

    // Получить информацию о файле
    public FileResponse getFileInfo(UUID id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Файл не найден с id: " + id));

        return mapToResponse(file);
    }

    // Удалить файл
    @Transactional
    public void deleteFile(UUID id) {
        File file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));

        // 2. Проверить права (автор или админ)
        User currentUser = getCurrentUser();
        boolean isOwner = file.getUploader().getId().equals(currentUser.getId());
        boolean hasPermission = currentUser.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(perm -> perm.name().equals("DELETE_FILE"));

        if (!isOwner && !hasPermission) {
            throw new AccessDeniedException("You don't have permission to delete this file");
        }

        // 3. Удалить файл с диска
        try {
            Path filePath = Paths.get(file.getFileUrl());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete physical file: " + e.getMessage(), e);
        }
        fileRepository.delete(file);
    }





    /**
     * Проверка, является ли пользователь владельцем файла (для @PreAuthorize)
     */
    public boolean isOwner(UUID fileId, UUID userId) {
        return fileRepository.existsByIdAndUploaderId(fileId, userId);
    }

    /**
     * Вспомогательный метод для маппинга File → FileResponse
     */
    private FileResponse mapToResponse(File file) {
        String uploaderName = file.getUploader().getFirstName() + " " + file.getUploader().getLastName();

        // Генерируем URL для скачивания
        String downloadUrl = "/api/files/download/" + file.getId();

        return new FileResponse(
                file.getId(),
                file.getFileName(),
                file.getFileType(),
                file.getSize(),
                uploaderName,
                file.getUploadedAt(),
                downloadUrl
        );
    }

    // Метод для сохранения файла с задачей
    @Transactional
    public void uploadFileForTask(MultipartFile multipartFile, Task task) {

        User currentUser = getCurrentUser();

        if (multipartFile.isEmpty()) throw new IllegalArgumentException("Файл пустой");

        String originalFileName = multipartFile.getOriginalFilename();
        String uniqueFileName = generateUniqueFileName(originalFileName);

        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"), uploaderDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(uniqueFileName);
            multipartFile.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла", e);
        }

        File file = File.builder()
                .fileName(originalFileName)
                .fileUrl(uploaderDir + "/" + uniqueFileName)
                .fileType(multipartFile.getContentType())
                .size(multipartFile.getSize())
                .uploader(currentUser)
                .uploadedAt(LocalDateTime.now())
                .task(task)
                .build();

        fileRepository.save(file);
    }

    // Получить файлы по ID задачи
    public List<FileResponse> getFilesByTaskId(UUID taskId) {
        List<File> files = fileRepository.findByTaskId(taskId);
        return files.stream().map(this::mapToResponse).toList();
    }

    // Подсчитать количество файлов у задачи
    public int countFilesByTaskId(UUID taskId) {
        return fileRepository.countByTaskId(taskId);
    }

    // Удалить файлы задачи
    @Transactional
    public void deleteFilesByTaskId(UUID taskId) {
        List<File> files = fileRepository.findByTaskId(taskId);
        for (File file : files) {
            try {
                Files.deleteIfExists(Paths.get(file.getFileUrl()));
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + file.getFileUrl());
            }
            fileRepository.delete(file);
        }
    }
    /**
     * Вспомогательный метод для генерации уникального имени файла
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        return UUID.randomUUID() + extension;
    }

}
