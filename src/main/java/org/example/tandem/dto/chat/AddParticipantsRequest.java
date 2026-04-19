package org.example.tandem.dto.chat;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddParticipantsRequest {
    private List<UUID> participantIds;
}
