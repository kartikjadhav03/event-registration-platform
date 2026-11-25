package com.event_registration_platform.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}