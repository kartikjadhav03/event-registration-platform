
package com.event_registration_platform.service;

import com.event_registration_platform.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    // Authentication
    UserResponseDTO registerUser(RegisterRequest request);

    String verifyEmail(String token);

    AuthResponse loginUser(LoginRequest request);

    AuthResponse refreshAccessToken(String refreshToken);

    String forgotPassword(ForgotPasswordRequest request);

    String resetPassword(ResetPasswordRequest request);

    // User Profile Management
    UserResponseDTO getMyProfile(String email);

    UserResponseDTO updateMyProfile(String email, String name);

    // Admin User Management with Pagination
    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    UserResponseDTO getUserById(Long id);
}
