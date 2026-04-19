package org.example.tandem.service;

import org.example.tandem.dto.chat.ChatMessage;
import org.example.tandem.dto.chat.ChatResponse;
import org.example.tandem.dto.chat.UserBrief;
import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.entity.Chat;
import org.example.tandem.entity.Message;
import org.example.tandem.entity.User;
import org.example.tandem.repository.ChatRepository;
import org.example.tandem.repository.MessageRepository;
import org.example.tandem.repository.UserRepository;
import org.example.tandem.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, UserRepository userRepository, FileService fileService, SimpMessagingTemplate messagingTemplate) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
        this.messagingTemplate = messagingTemplate;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new AccessDeniedException("Пользователь не авторизован");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    // ========== СОЗДАНИЕ ЧАТОВ ==========

    // Создать приватный чат
    @Transactional
    public ChatResponse createPrivateChat(UUID targetUserId) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + targetUserId));

        // Проверяем, существует ли уже чат между ними
        var existingChat = chatRepository.findPrivateChat(currentUser.getId(), targetUserId);
        if (existingChat.isPresent()) {
            return mapToChatResponse(existingChat.get(), currentUser.getId());
        }

        // Создаем новый приватный чат
        Chat chat = Chat.builder()
                .name(null)
                .type("PRIVATE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        chat.getUsers().add(currentUser);
        chat.getUsers().add(targetUser);

        Chat savedChat = chatRepository.save(chat);
        return mapToChatResponse(savedChat, currentUser.getId());
    }

    // Создать групповой чат
    @Transactional
    public ChatResponse createGroupChat(String name, List<UUID> participantIds) {
        User currentUser = getCurrentUser();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group chat name is required");
        }

        Chat chat = Chat.builder()
                .name(name)
                .type("GROUP")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Добавляем создателя
        chat.getUsers().add(currentUser);

        // Добавляем остальных участников
        for (UUID participantId : participantIds) {
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + participantId));
            chat.getUsers().add(participant);
        }

        Chat savedChat = chatRepository.save(chat);

        // Отправляем уведомление всем участникам о создании чата
        notifyParticipants(savedChat, "CHAT_CREATED");

        return mapToChatResponse(savedChat, currentUser.getId());
    }

    // ========== ПОЛУЧЕНИЕ ДАННЫХ ==========

    // Получить все чаты пользователя
    public Page<ChatResponse> getMyChats(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<Chat> chatsPage = chatRepository.findChatsByUserId(currentUser.getId(), pageable);
        return chatsPage.map(chat -> mapToChatResponse(chat, currentUser.getId()));
    }

    // Получить чат по ID
    public ChatResponse getChatById(UUID chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }
        return mapToChatResponse(chat, currentUser.getId());
    }

    // ========== УПРАВЛЕНИЕ УЧАСТНИКАМИ ==========

    // Добавить участников в групповой чат
    @Transactional
    public ChatResponse addParticipants(UUID chatId, List<UUID> participantIds) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }


        if (!"GROUP".equals(chat.getType())) {
            throw new IllegalArgumentException("Only group chats can have participants added");
        }

        for (UUID participantId : participantIds) {
            User participant = userRepository.findById(participantId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + participantId));
            chat.getUsers().add(participant);
        }

        chat.setUpdatedAt(LocalDateTime.now());
        Chat savedChat = chatRepository.save(chat);

        // Уведомление новых участников
        notifyParticipants(savedChat, "NEW_PARTICIPANTS");

        return mapToChatResponse(savedChat, currentUser.getId());
    }

    // Выйти из чата
    @Transactional
    public void leaveChat(UUID chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findByIdWithUsers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }


        chat.getUsers().remove(currentUser);

        // Если чат пустой, удаляем его
        if (chat.getUsers().isEmpty()) {
            chatRepository.delete(chat);
        } else {
            chat.setUpdatedAt(LocalDateTime.now());
            chatRepository.save(chat);
        }
    }

    // ========== СООБЩЕНИЯ ==========

    // Отправить сообщение (с файлами)
    @Transactional
    public ChatMessage sendMessage(UUID chatId, String content,
                                   String type, List<MultipartFile> files) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findByIdWithUsers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }

        Message message = Message.builder()
                .content(content)
                .type(type != null ? type : "TEXT")
                .createdAt(LocalDateTime.now())
                .chat(chat)
                .sender(currentUser)
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        // Обновляем updated_at чата
        chat.setUpdatedAt(LocalDateTime.now());
        chatRepository.save(chat);

        // Сохраняем файлы, если есть
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                fileService.uploadFileForMessage(file, savedMessage);
            }
        }

        ChatMessage chatMessage = mapToChatMessage(savedMessage);

        // Отправляем WebSocket всем участникам чата
        for (User participant : chat.getUsers()) {
            messagingTemplate.convertAndSendToUser(
                    participant.getId().toString(),
                    "/queue/messages",
                    chatMessage
            );
        }

        // Так же отправляем в топик чата
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, chatMessage);

        return chatMessage;
    }

    // Отправить текстовое сообщение (без файлов)
    public ChatMessage sendMessage(UUID chatId, String content) {
        return sendMessage(chatId, content, "TEXT", null);
    }

    // Получить сообщения чата
    public Page<ChatMessage> getChatMessages(UUID chatId, int page, int size) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findByIdWithUsers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }


        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messagesPage = messageRepository.findByChatIdOrderByCreatedAtDesc(chatId, pageable);

        return messagesPage.map(this::mapToChatMessage);
    }

    // Отметить сообщения как прочитанные
    @Transactional
    public void markMessagesAsRead(UUID chatId) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findByIdWithUsers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }


        messageRepository.markMessageAsRead(chatId, currentUser.getId());
    }

    // Поиск сообщений
    public Page<ChatMessage> searchMessages(UUID chatId, String keyword, int page, int size) {
        User currentUser = getCurrentUser();
        Chat chat = chatRepository.findByIdWithUsers(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found: " + chatId));

        if (!chatRepository.isParticipant(chatId, currentUser.getId())) {
            throw new AccessDeniedException("You are not a member of this chat");
        }


        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messagesPage = messageRepository.findByChatIdAndContentContainingIgnoreCase(chatId, keyword, pageable);
        return messagesPage.map(this::mapToChatMessage);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void notifyParticipants(Chat chat, String eventType) {
        for (User participant : chat.getUsers()) {
            messagingTemplate.convertAndSendToUser(
                    participant.getId().toString(),
                    "/queue/notifications",
                    Map.of("type", eventType, "chatId", chat.getId(), "chatName", chat.getName())
            );
        }
    }

    private ChatResponse mapToChatResponse(Chat chat, UUID currentUserId) {
        // Получаем последнее сообщение
        String lastMessage = null;
        LocalDateTime lastMessageAt = null;
        var lastMsgOpt = messageRepository.findLastMessage(chat.getId());
        if (lastMsgOpt.isPresent()) {
            Message lastMsg = lastMsgOpt.get();
            lastMessage = lastMsg.getContent();
            lastMessageAt = lastMsg.getCreatedAt();
        }

        // Считаем непрочитанные
        int unreadCount = messageRepository.countUnreadMessages(chat.getId(), currentUserId);

        // Формируем список участников
        List<UserBrief> participants = chat.getUsers().stream()
                .map(user -> new UserBrief(
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName(),
                        user.getAvatarUrl()
                ))
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .id(chat.getId())
                .name(chat.getName())
                .type(chat.getType())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                .participants(participants)
                .build();
    }

    private ChatMessage mapToChatMessage(Message message) {
        // Получаем файлы сообщения
        List<FileResponse> files = fileService.getFilesByMessageId(message.getId());

        return ChatMessage.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .type(message.getType())
                .timestamp(message.getCreatedAt())
                .isRead(message.getIsRead())
                .files(files)
                .build();
    }
}
