package org.example.tandem.repository;


import org.example.tandem.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    // Все файлы пользователя
    Page<File> findByUploaderIdOrderByUploadedAtDesc(UUID uploaderId, Pageable pageable);

    // Все файлы (с сортировкой)
    Page<File> findAllByOrderByUploadedAtDesc(Pageable pageable);

    // Проверка, является ли пользователь загрузившим
    boolean existsByIdAndUploaderId(UUID fileId, UUID uploaderId);
}
