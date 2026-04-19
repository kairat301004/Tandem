package org.example.tandem.repository;

import org.example.tandem.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.tags.form.SelectTag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Получить сообщения чата с пагинацией
    Page<Message> findByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    // Последнее сообщение в чате
    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLastMessage(@Param("chatId") UUID chatId);

    // Отметить сообщение как прочитанное
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true " +
            "WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.isRead = false")
    void markMessageAsRead(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    // Поиск сообщений
    Page<Message> findByChatIdAndContentContainingIgnoreCase(UUID chatId, String keyword, Pageable pageable);

    // Количество непрочитанных сообщений в чате для  конкретного пользователя
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId " +
            "AND m.sender.id != :userId AND m.isRead = false ")
    int countUnreadMessages(@Param("chatId") UUID chatId,
                            @Param("userId") UUID userId);

    // Количество всех непрочитанных сообщений пользователя по всем чатам
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id IN :chatIds " +
            "AND m.sender.id != :userId AND m.isRead = false ")
    int countTotalUnreadMessages(@Param("chatIds") List<UUID> chatIds,
                                 @Param("userId") UUID userId);
}
