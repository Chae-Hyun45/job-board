package com.jobboard.user.dto;

import jakarta.validation.constraints.Pattern;

public record RoleUpdateRequest(
        @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN 이어야 합니다.") String role
) {
}
