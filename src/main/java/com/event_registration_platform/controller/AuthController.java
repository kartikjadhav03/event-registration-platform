package com.event_registration_platform.controller;

import com.event_registration_platform.dto.RegisterRequest;
import com.event_registration_platform.dto.UserResponseDTO;
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
}