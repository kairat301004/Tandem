package org.example.tandem.controller;

import jakarta.validation.Valid;
import org.example.tandem.dto.chat.*;
import org.example.tandem.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ========== ЧАТЫ ==========

    // Получить все чаты пользователя
    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getMyChats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getMyChats(page, size));
    }

    // Получить чат по ID
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatResponse> getChatById(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatById(chatId));
    }

    // Создать приватный чат
    @PostMapping("/private/{userId}")
    public ResponseEntity<ChatResponse> createPrivateChat(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatService.createPrivateChat(userId));
    }

    // Создать групповой чат
    @PostMapping("/group")
    public ResponseEntity<ChatResponse> createGroupChat(@Valid @RequestBody CreateGroupChatRequest request) {
        return ResponseEntity.ok(chatService.createGroupChat(
                request.getName(),
                request.getParticipantIds()));
    }

    // Добавить участников в групповой чат
    @PostMapping("/{chatId}/participants")
    public ResponseEntity<ChatResponse> addParticipants(
            @PathVariable UUID chatId,
            @Valid @RequestBody AddParticipantsRequest request) {
        return ResponseEntity.ok(chatService.addParticipants(chatId, request.getParticipantIds()));
    }

    // Выйти из чата
    @GetMapping("/{chatId}/leave")
    public ResponseEntity<Void> leaveChat(@PathVariable UUID chatId) {
        chatService.leaveChat(chatId);
        return ResponseEntity.noContent().build();
    }

    // ========== СООБЩЕНИЯ ==========

    // Отправить сообщение (текст)
    @PostMapping("/messages")
    public ResponseEntity<ChatMessage> sendMessage(@Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request.getChatId(), request.getContent()));
    }

    // Отправить сообщение с файлами
    @PostMapping("/messages/with-file")
    public ResponseEntity<ChatMessage> sendMessageWithFiles (
            @RequestParam("chatId") UUID chatId,
            @RequestParam("content") String content,
            @RequestParam(value = "files") List<MultipartFile> files) {
        ChatMessage message = chatService.sendMessage(chatId, content, "TEXT", files);
        return ResponseEntity.ok(message);
    }

    // Получить сообщения чата
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<ChatMessage>> getChatMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getChatMessages(chatId, page, size));
    }

    // Отметить сообщения как прочитанные
    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID chatId) {
        chatService.markMessagesAsRead(chatId);
        return ResponseEntity.noContent().build();
    }

    // Поиск сообщений
    @GetMapping("/{chatId}/search")
    public ResponseEntity<Page<ChatMessage>> searchMessages(
            @PathVariable UUID chatId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.searchMessages(chatId, keyword, page, size));
    }
}
