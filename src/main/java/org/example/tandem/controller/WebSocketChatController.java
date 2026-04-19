package org.example.tandem.controller;

import org.example.tandem.dto.chat.ChatMessage;
import org.example.tandem.dto.chat.WebSocketMessage;
import org.example.tandem.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class WebSocketChatController {

    private final ChatService chatService;

    public WebSocketChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Отправка сообщения через WebSocket
    @MessageMapping("/chat/{chatId}/send")
    @SendTo("/topic/chat/{chatId}")
    public ChatMessage sendMessage(
            @DestinationVariable UUID chatId,
            @Payload String content) {
        return chatService.sendMessage(chatId, content);
    }

    // Отправка сообщения с типом
    @MessageMapping("/chat/{chatId}/send-typed")
    @SendTo("/topic/chat/{chatId}")
    public ChatMessage sendTypedMessage(
            @DestinationVariable UUID chatId,
            @Payload WebSocketMessage message) {
        return chatService.sendMessage(
                chatId,
                message.getContent(),
                message.getType(),
                null);
    }
}