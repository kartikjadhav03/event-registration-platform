
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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

    @Override
    public UserResponseDTO registerUser(RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.isVerified()) {
                throw new RuntimeException("Email already registered");
            }

            String token = UUID.randomUUID().toString();

            user.setVerificationToken(token);
            user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(15));

            User savedUser = userRepository.save(user);

            emailService.sendVerificationEmail(savedUser.getEmail(), token);

            return toUserResponse(savedUser);
        }

        user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ATTENDEE);

        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(15));

        User savedUser = userRepository.save(user);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return toUserResponse(savedUser);
    }

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

    @Override
    public AuthResponse loginUser(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        if (user.getRole() == null) {
            throw new RuntimeException("User role is not configured");
        }

        String accessToken = jwtUtil.generateToken(
                user.getEmail(), user.getRole().toString());

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        user.setRefreshToken(hashToken(refreshToken));
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank() ||
                !jwtUtil.isRefreshTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = normalizeEmail(jwtUtil.extractEmail(refreshToken));

        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!user.isVerified()) {
            throw new RuntimeException("Email is not verified");
        }

        if (user.getRole() == null) {
            throw new RuntimeException("User role is not configured");
        }

        String incomingHash = hashToken(refreshToken);
        String storedHash = user.getRefreshToken();

        if (storedHash == null ||
                !MessageDigest.isEqual(
                        incomingHash.getBytes(StandardCharsets.UTF_8),
                        storedHash.getBytes(StandardCharsets.UTF_8))) {
            throw new RuntimeException("Refresh token mismatch or already used");
        }

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(), user.getRole().toString());

        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        user.setRefreshToken(hashToken(newRefreshToken));
        userRepository.save(user);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            String token = passwordResetService.generateResetToken(user.getEmail());
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        }

        return "If the email is registered, a password reset email has been sent";
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {

        String email = passwordResetService.getEmailByToken(request.getToken());

        if (email == null) {
            throw new RuntimeException("Invalid or expired token");
        }

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRefreshToken(null);

        userRepository.save(user);
        passwordResetService.deleteToken(request.getToken());

        return "Password reset successful";
    }

    @Override
    public UserResponseDTO getMyProfile(String email) {

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toUserResponse(user);
    }

    @Override
    public UserResponseDTO updateMyProfile(String email, String name) {

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(name.trim());

        return toUserResponse(userRepository.save(user));
    }

    @Override
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {

        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return toUserResponse(user);
    }

    private UserResponseDTO toUserResponse(User user) {

        return new UserResponseDTO(
                user.getName(),
                user.getEmail(),
                user.getRole().toString());
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }

        return email.trim().toLowerCase();
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
