package org.example.tandem.repository;


import org.example.tandem.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {

    // Все файлы пользователя
    Page<File> findByUploaderIdOrderByUploadedAtDesc(UUID uploaderId, Pageable pageable);

    // Все файлы (с сортировкой)
    Page<File> findAllByOrderByUploadedAtDesc(Pageable pageable);

    // Проверка, является ли пользователь загрузившим
    boolean existsByIdAndUploaderId(UUID fileId, UUID uploaderId);

    //Найти все файлы по ID новости
    List<File> findByNewsId(UUID newsId);

    List<File> findByTaskId(UUID taskId);
    int countByTaskId(UUID taskId);

    //Файлы прикрепленные к сообщению
    List<File> findByMessageId(UUID messageId);

    // Количество файлов у сообщения
    @Query("SELECT COUNT(f) FROM File f WHERE f.message.id = :messageId")
    int countByMessageId(@Param("messageId") UUID messageId);

    // Удалить файлы сообщения
    @Modifying
    @Transactional
    @Query("DELETE FROM File f WHERE f.message.id = :messageId")
    void deleteByMessageId(@Param("messageId") UUID messageId);
}
