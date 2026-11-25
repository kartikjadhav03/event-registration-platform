
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

    // Helper method
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

    // 1. Register new user
    @Test
    void registerUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "Kartik",
                "kartik@gmail.com",
                "Password123"
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
                eq("kartik@gmail.com"),
                anyString()
        );
    }

    // 2. Existing unverified user registration
    @Test
    void registerExistingUnverifiedUser() {

        User user = createUser();
        user.setVerified(false);

        RegisterRequest request = new RegisterRequest(
                "New Name",
                "kartik@gmail.com",
                "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO response = userService.registerUser(request);

        assertEquals("Kartik", response.getName());
        assertNotNull(user.getVerificationToken());
        assertNotNull(user.getVerificationTokenExpiry());

        verify(emailService).sendVerificationEmail(
                eq(user.getEmail()),
                anyString()
        );
    }

    // 3. Reject already verified account
    @Test
    void registerExistingVerifiedUserThrowsException() {

        User user = createUser();

        RegisterRequest request = new RegisterRequest(
                "Kartik",
                "kartik@gmail.com",
                "Password123"
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

    // 4. Verify email successfully
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

    // 5. Invalid verification token
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

    // 6. Expired verification token
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

    // 7. Login successfully
    @Test
    void loginUserSuccessfully() {

        User user = createUser();

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com",
                "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtUtil.generateToken("kartik@gmail.com", "ATTENDEE"))
                .thenReturn("access-token");

        when(jwtUtil.generateRefreshToken("kartik@gmail.com"))
                .thenReturn("new-refresh-token");

        when(userRepository.save(user)).thenReturn(user);

        AuthResponse response = userService.loginUser(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        assertEquals("new-refresh-token", user.getRefreshToken());

        verify(userRepository).save(user);
    }

    // 8. Login with unknown email
    @Test
    void loginWithUnknownEmail() {

        LoginRequest request = new LoginRequest(
                "unknown@gmail.com",
                "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginUser(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );
    }

    // 9. Login with wrong password
    @Test
    void loginWithWrongPassword() {

        User user = createUser();

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com",
                "WrongPassword"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "encoded-password"
        )).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.loginUser(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );
    }

    // 10. Login before email verification
    @Test
    void loginWithUnverifiedEmail() {

        User user = createUser();
        user.setVerified(false);

        LoginRequest request = new LoginRequest(
                "kartik@gmail.com",
                "Password123"
        );

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123",
                "encoded-password"
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

    // 11. Refresh access token successfully
    @Test
    void refreshAccessTokenSuccessfully() {

        User user = createUser();

        when(jwtUtil.isRefreshTokenValid("old-refresh-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("old-refresh-token"))
                .thenReturn("kartik@gmail.com");

        when(userRepository.findByEmail("kartik@gmail.com"))
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

        verify(userRepository).save(user);
    }

    // 12. Invalid refresh token
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

    // 13. Refresh token mismatch
    @Test
    void refreshWithMismatchedToken() {

        User user = createUser();

        when(jwtUtil.isRefreshTokenValid("different-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("different-token"))
                .thenReturn("kartik@gmail.com");

        when(userRepository.findByEmail("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("different-token")
        );

        assertEquals(
                "Refresh token mismatch or already used",
                exception.getMessage()
        );
    }

    // 14. Refresh token for unknown user
    @Test
    void refreshWhenUserDoesNotExist() {

        when(jwtUtil.isRefreshTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtUtil.extractEmail("valid-token"))
                .thenReturn("unknown@gmail.com");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.refreshAccessToken("valid-token")
        );

        assertEquals("User not found", exception.getMessage());
    }

    // 15. Forgot password for existing user
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
                "kartik@gmail.com",
                "reset-token"
        );
    }

    // 16. Forgot password for unknown user
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

    // 17. Reset password successfully
    @Test
    void resetPasswordSuccessfully() {

        User user = createUser();

        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token",
                "NewPassword123"
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

    // 18. Reset password with invalid token
    @Test
    void resetPasswordWithInvalidToken() {

        ResetPasswordRequest request = new ResetPasswordRequest(
                "invalid-token",
                "NewPassword123"
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

    // 19. Reset password when user does not exist
    @Test
    void resetPasswordWhenUserDoesNotExist() {

        ResetPasswordRequest request = new ResetPasswordRequest(
                "reset-token",
                "NewPassword123"
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

    // 20. Get profile successfully
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

    // 21. Get profile when user does not exist
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

    // 22. Update profile successfully
    @Test
    void updateMyProfileSuccessfully() {

        User user = createUser();

        when(userRepository.findByEmail("kartik@gmail.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user)).thenReturn(user);

        UserResponseDTO response = userService.updateMyProfile(
                "kartik@gmail.com",
                "Updated Kartik"
        );

        assertEquals("Updated Kartik", response.getName());
        assertEquals("kartik@gmail.com", response.getEmail());

        verify(userRepository).save(user);
    }

    // 23. Get all users with pagination
    @Test
    void getAllUsersSuccessfully() {

        User user = createUser();

        PageRequest pageable = PageRequest.of(0, 10);

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        Page<UserResponseDTO> response = userService.getAllUsers(
                pageable
        );

        assertEquals(1, response.getTotalElements());
        assertEquals("Kartik", response.getContent().get(0).getName());
        assertEquals(
                "kartik@gmail.com",
                response.getContent().get(0).getEmail()
        );
    }

    // 24. Get user by ID successfully
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

    // 25. Get user by ID when user does not exist
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
