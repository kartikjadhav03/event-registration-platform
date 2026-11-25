package com.event_registration_platform.controller;

import com.event_registration_platform.dto.*;
import com.event_registration_platform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody RegisterRequest request) {
        return userService.registerUser(request);
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token) {
        return userService.verifyEmail(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.loginUser(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestParam String refreshToken) {
        return userService.refreshAccessToken(refreshToken);
    }
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }
}