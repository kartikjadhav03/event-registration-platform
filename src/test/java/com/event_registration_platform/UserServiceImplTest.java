
package com.event_registration_platform;

import com.event_registration_platform.dto.*;
import com.event_registration_platform.entity.Role;
import com.event_registration_platform.entity.User;
import com.event_registration_platform.repository.UserRepository;
import com.event_registration_platform.service.*;
import com.event_registration_platform.util.JwtUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private UserServiceImpl userService;

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Kartik");
        user.setEmail("kartik@gmail.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ATTENDEE);
        user.setVerified(true);
        user.setRefreshToken("old-refresh-token");
        return user;
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void registerUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "Kartik", "kartik@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO response = userService.registerUser(request);

        assertEquals("Kartik", response.getName());
        assertEquals("kartik@gmail.com", response.getEmail());
        assertEquals("ATTENDEE", response.getRole());

        verify(passwordEncoder).encode("Password123");
        verify(emailService).sendVerificationEmail(
                eq("kartik@gmail.com"), anyString()
        );
    }

    @Test
    void registerExistingUnverifiedUser() {
        User user = createUser();
        user.setVerified(false);

        RegisterRequest request = new RegisterRequest(
                "New Name", "kartik@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO response = userService.registerUser(request);

        assertEquals("Kartik", response.getName());
        assertNotNull(user.getVerificationToken());
        assertNotNull(user.getVerificationTokenExpiry());

        verify(emailService).sendVerificationEmail(
                eq(user.getEmail()), anyString()
        );
    }

    @Test
    void registerExistingVerifiedUserThrowsException() {
        User user = createUser();

        RegisterRequest request = new RegisterRequest(
                "Kartik", "kartik@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerUser(request)
        );

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void verifyEmailSuccessfully() {
        User user = createUser();
        user.setVerified(false);
        user.setVerificationToken("valid-token");
        user.setVerificationTokenExpiry(
                LocalDateTime.now().plusMinutes(10)
        );

        when(userRepository.findByVerificationToken("valid-token"))
                .thenReturn(Optional.of(user));

        String response = userService.verifyEmail("valid-token");

        assertEquals("Email verified successfully", response);
        assertTrue(user.isVerified());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());

        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailWithInvalidToken() {
        when(userRepository.findByVerificationToken("wrong-token"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.verifyEmail("wrong-token")
        );

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void verifyEmailWithExpiredToken() {
        User user = createUser();
        user.setVerificationTokenExpiry(
                LocalDateTime.now().minusMinutes(5)
        );

        when(userRepository.findByVerificationToken("expired-token"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.verifyEmail("expired-token")
        );

        assertEquals("Token expired", exception.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginUserSuccessfully() {
        User user = createUser();

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123", "encoded-password"
        )).thenReturn(true);

        when(jwtUtil.generateToken("kartik@gmail.com", "ATTENDEE"))
                .thenReturn("access-token");

        when(jwtUtil.generateRefreshToken("kartik@gmail.com"))
                .thenReturn("new-refresh-token");

        when(userRepository.save(user)).thenReturn(user);

        AuthResponse response = userService.loginUser(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertEquals(
                hashToken("new-refresh-token"),
                user.getRefreshToken()
        );

        assertNotEquals("new-refresh-token", user.getRefreshToken());

        verify(userRepository).save(user);
    }

    @Test
    void loginWithUnknownEmail() {
        LoginRequest request = new LoginRequest(
                "unknown@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginUser(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginWithWrongPassword() {
        User user = createUser();

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com", "WrongPassword"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword", "encoded-password"
        )).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginUser(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginWithUnverifiedEmail() {
        User user = createUser();
        user.setVerified(false);

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com", "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123", "encoded-password"
        )).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginUser(request)
        );

        assertEquals(
                "Please verify your email first",
                exception.getMessage()
        );
    }

    @Test
    void refreshAccessTokenSuccessfully() {
        User user = createUser();

        user.setRefreshToken(hashToken("old-refresh-token"));

        when(jwtUtil.isRefreshTokenValid("old-refresh-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("old-refresh-token"))
                .thenReturn("kartik@gmail.com");

        when(userRepository.findByEmailForUpdate("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        when(jwtUtil.generateToken("kartik@gmail.com", "ATTENDEE"))
                .thenReturn("new-access-token");

        when(jwtUtil.generateRefreshToken("kartik@gmail.com"))
                .thenReturn("new-refresh-token");

        when(userRepository.save(user)).thenReturn(user);

        AuthResponse response = userService.refreshAccessToken(
                "old-refresh-token"
        );

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertEquals(
                hashToken("new-refresh-token"),
                user.getRefreshToken()
        );

        verify(userRepository).findByEmailForUpdate("kartik@gmail.com");
        verify(userRepository).save(user);
    }

    @Test
    void refreshWithInvalidToken() {
        when(jwtUtil.isRefreshTokenValid("invalid-token"))
                .thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("invalid-token")
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshWithMismatchedToken() {
        User user = createUser();

        user.setRefreshToken(hashToken("old-refresh-token"));

        when(jwtUtil.isRefreshTokenValid("different-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("different-token"))
                .thenReturn("kartik@gmail.com");

        when(userRepository.findByEmailForUpdate("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("different-token")
        );

        assertEquals(
                "Refresh token mismatch or already used",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshWhenUserDoesNotExist() {
        when(jwtUtil.isRefreshTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("valid-token"))
                .thenReturn("unknown@gmail.com");

        when(userRepository.findByEmailForUpdate("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("valid-token")
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void forgotPasswordForExistingUser() {
        User user = createUser();

        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "kartik@gmail.com"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordResetService.generateResetToken(user.getEmail()))
                .thenReturn("reset-token");

        String response = userService.forgotPassword(request);

        assertEquals(
                "If the email is registered, a password reset email has been sent",
                response
        );

        verify(emailService).sendPasswordResetEmail(
                "kartik@gmail.com", "reset-token"
        );
    }

    @Test
    void forgotPasswordForUnknownUser() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "unknown@gmail.com"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        String response = userService.forgotPassword(request);

        assertEquals(
                "If the email is registered, a password reset email has been sent",
                response
        );

        verifyNoInteractions(passwordResetService);
        verifyNoInteractions(emailService);
    }

    @Test
    void resetPasswordSuccessfully() {
        User user = createUser();

        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token", "NewPassword123"
        );

        when(passwordResetService.getEmailByToken("reset-token"))
                .thenReturn("kartik@gmail.com");

        when(userRepository.findByEmail("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("new-encoded-password");

        when(userRepository.save(user)).thenReturn(user);

        String response = userService.resetPassword(request);

        assertEquals("Password reset successful", response);
        assertEquals("new-encoded-password", user.getPassword());
        assertNull(user.getRefreshToken());

        verify(passwordResetService).deleteToken("reset-token");
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordWithInvalidToken() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "invalid-token", "NewPassword123"
        );

        when(passwordResetService.getEmailByToken("invalid-token"))
                .thenReturn(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.resetPassword(request)
        );

        assertEquals(
                "Invalid or expired token",
                exception.getMessage()
        );
    }

    @Test
    void resetPasswordWhenUserDoesNotExist() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token", "NewPassword123"
        );

        when(passwordResetService.getEmailByToken("reset-token"))
                .thenReturn("unknown@gmail.com");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.resetPassword(request)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getMyProfileSuccessfully() {
        User user = createUser();

        when(userRepository.findByEmail("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getMyProfile(
                "kartik@gmail.com"
        );

        assertEquals("Kartik", response.getName());
        assertEquals("kartik@gmail.com", response.getEmail());
        assertEquals("ATTENDEE", response.getRole());
    }

    @Test
    void getMyProfileWhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getMyProfile("unknown@gmail.com")
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void updateMyProfileSuccessfully() {
        User user = createUser();

        when(userRepository.findByEmail("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO response = userService.updateMyProfile(
                "kartik@gmail.com", "Updated Kartik"
        );

        assertEquals("Updated Kartik", response.getName());
        assertEquals("kartik@gmail.com", response.getEmail());

        verify(userRepository).save(user);
    }

    @Test
    void getAllUsersSuccessfully() {
        User user = createUser();

        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        Page<UserResponseDTO> response = userService.getAllUsers(pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Kartik", response.getContent().get(0).getName());
        assertEquals(
                "kartik@gmail.com",
                response.getContent().get(0).getEmail()
        );
    }

    @Test
    void getUserByIdSuccessfully() {
        User user = createUser();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getUserById(1L);

        assertEquals("Kartik", response.getName());
        assertEquals("kartik@gmail.com", response.getEmail());
        assertEquals("ATTENDEE", response.getRole());
    }

    @Test
    void getUserByIdWhenUserDoesNotExist() {
        when(userRepository.findById(100L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(100L)
        );

        assertEquals("User not found", exception.getMessage());
    }
}
