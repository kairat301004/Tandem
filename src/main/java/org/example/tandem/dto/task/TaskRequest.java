package org.example.tandem.dto.task;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.tandem.enums.TaskPriority;
import org.example.tandem.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Заголовок обязателен")
    private String title;

    private String description;

    @NotNull(message = "Статус не может быть пустым")
    private TaskStatus status;

    @NotNull(message = "Приоритет не может быть  пустым")
    private TaskPriority priority;

    private LocalDateTime deadline;

    private UUID assignedId;

}
