package org.example.tandem.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String position;
    private String phone;
    private String status;
    private String departmentName;
    private UUID departmentId;
    private Set<String> roleNames;
    private LocalDateTime createdAt;
}