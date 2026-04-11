package org.example.tandem.repository;

import org.example.tandem.entity.Task;
import org.example.tandem.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    //Все задачи с пагинацией
    Page<Task> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Задачи пользователя (как создатель или исполнитель)
    @Query("SELECT t FROM Task t WHERE t.creator.id = :userId OR t.assigned.id = :userId")
    Page<Task> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Задачи по статусу
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // Задачи исполнителя
    Page<Task> findByAssignedId(UUID assignedId, Pageable pageable);

    // Проверка, является ли пользователь создателем
    boolean existsByIdAndCreatorId(UUID taskId, UUID userId);
}
