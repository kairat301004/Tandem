package org.example.tandem.dto.news;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponse implements Serializable {
    private UUID id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private Boolean isPinned;
}