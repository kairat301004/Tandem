package org.example.tandem.dto.chat;

import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrivateChatRequest {
    private UUID targetUserId;
}
