package com.labourse.auth.dto;

import com.labourse.auth.entity.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectRoleRequest {
    @NotNull
    private UserType userType;
}
