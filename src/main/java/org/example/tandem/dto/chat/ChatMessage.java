package org.example.tandem.dto.chat;

import lombok.*;
import org.example.tandem.dto.file.FileResponse;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

    private UUID id;
    private UUID chatId;
    private UUID senderId;
    private String senderName;
    private String content;
    private String type;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private List<FileResponse> files;

}
