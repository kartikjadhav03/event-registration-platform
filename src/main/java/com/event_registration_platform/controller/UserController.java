
package com.event_registration_platform.controller;

import com.event_registration_platform.dto.UserResponseDTO;
import com.event_registration_platform.dto.UpdateProfileRequest;
import com.event_registration_platform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get logged-in user's profile
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyProfile(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(userService.getMyProfile(email));
    }

    // Update logged-in user's profile
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                userService.updateMyProfile(email, request.getName())
        );
    }

    // Admin: Get all users with Pagination
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            Pageable pageable) {

        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    // Admin: Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(userService.getUserById(id));
    }
}
