package org.example.tandem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tandem.entity.Notification;
import org.example.tandem.entity.User;
import org.example.tandem.repository.NotificationRepository;
import org.example.tandem.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    // Основной метод отправки уведомления
    @Transactional
    public void sendNotification(User recipient, String type, String title, String content, String link, Map<String, Object> additionalData) {
        try {
            // Создаем payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("content", content);
            payload.put("link", link);
            payload.put("type", type);
            payload.put("timestamp", LocalDateTime.now());
            if (additionalData != null) {
                payload.putAll(additionalData);
            }

            String payloadJson = objectMapper.writeValueAsString(payload);

            //Сохраняем в БД
            Notification notification = Notification.builder()
                    .user(recipient)
                    .type(type)
                    .payload(payloadJson)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            Notification saved = notificationRepository.save(notification);

            // Отправляем через WebSocket
            messagingTemplate.convertAndSendToUser(
                    recipient.getId().toString(),
                    "/queue/notifications",
                    saved
            );

        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }

    // Упрощенный метод
    public void sendNotification(User recipient, String type, String title, String content, String link) {
        sendNotification(recipient, type, title, content, link, null);
    }

    // Отправка уведомления всем пользователям (для новостей)
    @Transactional
    public void sendNotificationToAllUsers(String type, String title, String content, String link) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            sendNotification(user, type, title, content, link);
        }
    }

    // Отправка уведомления группе пользователей

    @Transactional
    public void sendNotificationToUsers(List<User> recipients, String type, String title, String content, String link) {
        for (User user : recipients) {
            sendNotification(user, type, title, content, link);
        }
    }

    // Получить уведомления пользователя
    public Page<Notification> getUserNotifications(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    // Получить непрочитанные уведомления
    public Page<Notification> getUnreadNotifications(UUID userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findUnreadByUser(user, pageable);
    }

    // Количество непрочитанных
    public int getUnreadCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    // Отметить все как прочитанные
    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationRepository.markAllAsReadByUser(user);
    }
}
