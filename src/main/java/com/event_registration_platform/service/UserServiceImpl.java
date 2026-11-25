
package com.event_registration_platform.service;

import com.event_registration_platform.dto.*;
import com.event_registration_platform.entity.Role;
import com.event_registration_platform.entity.User;
import com.event_registration_platform.repository.UserRepository;
import com.event_registration_platform.util.JwtUtil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtUtil jwtUtil,
            PasswordResetService passwordResetService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    // Register User
    @Override
    public UserResponseDTO registerUser(RegisterRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Existing account
        if (user != null) {

            // Already verified account
            if (user.isVerified()) {
                throw new RuntimeException("Email already registered");
            }

            // Generate fresh verification token
            String token = UUID.randomUUID().toString();

            user.setVerificationToken(token);
            user.setVerificationTokenExpiry(
                    LocalDateTime.now().plusMinutes(15)
            );

            User savedUser = userRepository.save(user);

            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    token
            );

            return new UserResponseDTO(
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getRole().toString()
            );
        }

        // New account
        user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ATTENDEE);

        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(
                LocalDateTime.now().plusMinutes(15)
        );

        User savedUser = userRepository.save(user);

        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                token
        );

        return new UserResponseDTO(
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().toString()
        );
    }

    // Verify Email
    @Override
    public String verifyEmail(String token) {

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getVerificationTokenExpiry() == null ||
                user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Token expired");
        }

        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        userRepository.save(user);

        return "Email verified successfully";
    }

    // Login User
    @Override
    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        // Check email verification
        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().toString()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getEmail()
        );

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    // Refresh Access Token
    @Override
    public AuthResponse refreshAccessToken(String refreshToken) {

        if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtUtil.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new RuntimeException(
                    "Refresh token mismatch or already used"
            );
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Email is not verified");
        }

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().toString()
        );

        String newRefreshToken = jwtUtil.generateRefreshToken(
                user.getEmail()
        );

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    // Forgot Password
    @Override
    public String forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Do not reveal whether email exists
        if (user != null) {

            String token = passwordResetService.generateResetToken(
                    user.getEmail()
            );

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    token
            );
        }

        return "If the email is registered, a password reset email has been sent";
    }

    // Reset Password
    @Override
    public String resetPassword(ResetPasswordRequest request) {

        String email = passwordResetService.getEmailByToken(
                request.getToken()
        );

        if (email == null) {
            throw new RuntimeException("Invalid or expired token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // Invalidate old refresh token after password reset
        user.setRefreshToken(null);

        userRepository.save(user);

        passwordResetService.deleteToken(request.getToken());

        return "Password reset successful";
    }

    // Get My Profile
    @Override
    public UserResponseDTO getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return new UserResponseDTO(
                user.getName(),
                user.getEmail(),
                user.getRole().toString()
        );
    }

    // Update My Profile
    @Override
    public UserResponseDTO updateMyProfile(
            String email,
            String name) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setName(name);

        User updatedUser = userRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole().toString()
        );
    }

    // Admin: Get All Users with Pagination
    @Override
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable)
                .map(user -> new UserResponseDTO(
                        user.getName(),
                        user.getEmail(),
                        user.getRole().toString()
                ));
    }

    // Admin: Get User By ID
    @Override
    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return new UserResponseDTO(
                user.getName(),
                user.getEmail(),
                user.getRole().toString()
        );
    }
}
