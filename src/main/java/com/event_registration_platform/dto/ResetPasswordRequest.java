package com.event_registration_platform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8)
    private String newPassword;
}