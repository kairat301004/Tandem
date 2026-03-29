package org.example.tandem.dto.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileResponse {
    private UUID id;
    private String fileName;
    private String fileType;
    private Long size;
    private String uploaderName;  // имя загрузившего
    private LocalDateTime uploadedAt;
    private String downloadUrl;    // URL для скачивания
}
