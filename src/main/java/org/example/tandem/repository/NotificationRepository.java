package org.example.tandem.repository;

import org.example.tandem.entity.Notification;
import org.example.tandem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Уведомления пользователя с пагинацией
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Непрочитанные уведомления пользователя
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUser(@Param("user") User user, Pageable pageable);

    // Количество непрочитанных уведомлений
    int countByUserAndIsReadFalse(User user);

    // Отметить все как прочитанные
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadByUser(@Param("user") User user);

    // Удалить старые уведомления (старше 30 дней)
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :date")
    void deleteOldNotifications(@Param("date") LocalDateTime date);
}
