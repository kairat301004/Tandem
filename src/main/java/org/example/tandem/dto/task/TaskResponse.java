package org.example.tandem.dto.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tandem.dto.file.FileResponse;
import org.example.tandem.enums.TaskPriority;
import org.example.tandem.enums.TaskStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse implements Serializable {

    private UUID id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String creatorName;
    private String assignedName;
    private List<FileResponse> files;
}
