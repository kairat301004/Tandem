package org.example.tandem.repository;

import org.example.tandem.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    // Все чаты пользователя
    @Query("SELECT c FROM Chat c JOIN c.users u WHERE u.id = :userId")
    Page<Chat> findChatsByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Приватный чат между двумя пользователями
    @Query("SELECT c FROM Chat c JOIN c.users u1 JOIN c.users u2 " +
            "WHERE c.type = 'PRIVATE' AND u1.id = :user1Id AND u2.id = :user2Id " +
            "AND SIZE(c.users) = 2")
    Optional<Chat> findPrivateChat(@Param("user1Id") UUID user1Id,
                                   @Param("user2Id") UUID user2Id);

    // Проверка, является ли пользователь участником чата
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Chat c JOIN c.users u WHERE c.id = :chatId AND u.id = :userId")
    boolean isParticipant(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    // Найти чат по ID с загрузкой участников (для производительности)
    @Query("SELECT DISTINCT c FROM Chat c LEFT JOIN FETCH c.users u WHERE c.id = :chatId")
    Optional<Chat> findByIdWithUsers(@Param("chatId") UUID chatId);
}
