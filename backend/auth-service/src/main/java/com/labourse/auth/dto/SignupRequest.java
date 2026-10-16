package com.labourse.auth.dto;

import com.labourse.auth.entity.UserType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String mobile;

    // min 8 chars, 1 upper, 1 lower, 1 digit
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
             message = "Password must be 8+ chars with upper, lower and a digit")
    private String password;

    @NotNull
    private UserType userType;
}
