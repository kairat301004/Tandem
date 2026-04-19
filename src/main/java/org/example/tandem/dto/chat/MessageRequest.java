package org.example.tandem.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    @NotNull(message = "Chat ID is required")
    private UUID chatId;

    @NotBlank(message = "Message content is required")
    private String content;

    private String type; // TEXT, IMAGE, FILE
}
