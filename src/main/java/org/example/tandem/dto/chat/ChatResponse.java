package org.example.tandem.dto.chat;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {
    private UUID id;
    private String name;
    private String type;  // PRIVATE, GROUP
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private List<UserBrief> participants;

}

