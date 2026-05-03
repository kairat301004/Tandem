package org.example.tandem.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String position;
    private String phone;
    private String avatarUrl;
    private String status;
    private DepartmentInfo department;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentInfo {
        private UUID id;
        private String name;
    }
}