package org.example.tandem.controller;


import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    // Загрузка файла
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        FileResponse response = fileService.uploadFile(file);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    //Скачивание файла
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        Resource resource = fileService.downloadFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    //Получить список файлов
    @GetMapping
    public ResponseEntity<Page<FileResponse>> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(fileService.getALlFiles(page, size));
    }

    // Получить информацию о файле
    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileInfo(@PathVariable UUID id) {
        return ResponseEntity.ok(fileService.getFileInfo(id));
    }

    // Получить файлы пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<FileResponse>> getFilesByUser (
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(fileService.getFilesByUser(userId, page, size));
    }

    //Удалить файл
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_FILE') or @fileService.isOwner(#id, authentication.principal.user.id)")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
