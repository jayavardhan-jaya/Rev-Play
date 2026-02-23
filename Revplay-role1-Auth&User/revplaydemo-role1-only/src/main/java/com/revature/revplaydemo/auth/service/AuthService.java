package com.revature.revplaydemo.auth.service;

import com.revature.revplaydemo.auth.dto.request.ChangePasswordRequest;
import com.revature.revplaydemo.auth.dto.request.ForgotPasswordRequest;
import com.revature.revplaydemo.auth.dto.request.LoginRequest;
import com.revature.revplaydemo.auth.dto.request.RefreshTokenRequest;
import com.revature.revplaydemo.auth.dto.request.RegisterRequest;
import com.revature.revplaydemo.auth.dto.request.ResetPasswordRequest;
import com.revature.revplaydemo.auth.dto.response.AuthTokenResponse;
import com.revature.revplaydemo.auth.dto.response.ForgotPasswordResponse;
import com.revature.revplaydemo.auth.dto.response.SimpleMessageResponse;
import com.revature.revplaydemo.auth.dto.response.UserResponse;
import com.revature.revplaydemo.auth.entity.PasswordResetTokenEntity;
import com.revature.revplaydemo.auth.entity.UserEntity;
import com.revature.revplaydemo.auth.entity.UserProfileEntity;
import com.revature.revplaydemo.auth.enums.UserRole;
import com.revature.revplaydemo.auth.exception.AuthConflictException;
import com.revature.revplaydemo.auth.exception.AuthNotFoundException;
import com.revature.revplaydemo.auth.exception.AuthUnauthorizedException;
import com.revature.revplaydemo.auth.exception.AuthValidationException;
import com.revature.revplaydemo.auth.repository.PasswordResetTokenRepository;
import com.revature.revplaydemo.auth.repository.UserProfileRepository;
import com.revature.revplaydemo.auth.repository.UserRepository;
import com.revature.revplaydemo.auth.security.AuthenticatedUserPrincipal;
import com.revature.revplaydemo.auth.security.JwtProperties;
import com.revature.revplaydemo.auth.security.JwtService;
import com.revature.revplaydemo.auth.security.TokenRevocationService;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenRevocationService tokenRevocationService;
    private final AdminAuditLogService adminAuditLogService;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            TokenRevocationService tokenRevocationService,
            AdminAuditLogService adminAuditLogService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenRevocationService = tokenRevocationService;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AuthConflictException("Email already exists");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new AuthConflictException("Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.from(request.role()));
        user.setIsActive(Boolean.TRUE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        UserEntity savedUser = userRepository.save(user);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(savedUser.getUserId());
        profile.setFullName(request.fullName().trim());
        profile.setBio(null);
        profile.setProfilePictureUrl(null);
        profile.setCountry(null);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);

        return buildTokenResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        UserEntity user = resolveUserByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new AuthUnauthorizedException("Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthUnauthorizedException("Account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthUnauthorizedException("Invalid credentials");
        }
        return buildTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isRefreshToken(request.refreshToken())) {
            throw new AuthUnauthorizedException("Invalid refresh token");
        }
        AuthenticatedUserPrincipal principal = jwtService.toPrincipal(request.refreshToken());
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new AuthUnauthorizedException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthUnauthorizedException("Account is deactivated");
        }
        return buildTokenResponse(user);
    }

    public SimpleMessageResponse logout(String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            tokenRevocationService.revoke(bearerToken, jwtService.getExpiry(bearerToken));
        }
        return new SimpleMessageResponse("Logged out successfully");
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new AuthNotFoundException("User not found for the given email"));

        passwordResetTokenRepository.deleteByExpiresAtBeforeOrUsedAtIsNotNull(Instant.now());

        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUserId(user.getUserId());
        tokenEntity.setToken(generateResetToken());
        tokenEntity.setCreatedAt(Instant.now());
        tokenEntity.setExpiresAt(Instant.now().plusSeconds(3600));
        tokenEntity.setUsedAt(null);
        PasswordResetTokenEntity savedToken = passwordResetTokenRepository.save(tokenEntity);

        return new ForgotPasswordResponse(
                "Password reset token generated",
                savedToken.getToken(),
                savedToken.getExpiresAt()
        );
    }

    @Transactional
    public SimpleMessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity tokenEntity = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new AuthValidationException("Invalid reset token"));
        if (tokenEntity.getUsedAt() != null || tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthValidationException("Reset token is expired or already used");
        }

        UserEntity user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        tokenEntity.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(tokenEntity);

        adminAuditLogService.log(
                user.getUserId(),
                "PASSWORD_RESET",
                "users",
                user.getUserId(),
                "Password reset completed via reset token"
        );

        return new SimpleMessageResponse("Password reset successful");
    }

    @Transactional
    public SimpleMessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthValidationException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        adminAuditLogService.log(
                userId,
                "PASSWORD_CHANGE",
                "users",
                userId,
                "Password changed by authenticated user"
        );

        return new SimpleMessageResponse("Password changed successfully");
    }

    private AuthTokenResponse buildTokenResponse(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthTokenResponse(
                "Bearer",
                accessToken,
                jwtProperties.getAccessTokenExpirationSeconds(),
                refreshToken,
                jwtProperties.getRefreshTokenExpirationSeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Optional<UserEntity> resolveUserByUsernameOrEmail(String usernameOrEmail) {
        String input = usernameOrEmail.trim();
        if (input.contains("@")) {
            return userRepository.findByEmailIgnoreCase(input);
        }
        return userRepository.findByUsernameIgnoreCase(input);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new AuthValidationException("Register request is required");
        }
        if (request.password() != null && request.password().toLowerCase().contains("password")) {
            throw new AuthValidationException("Password is too weak");
        }
    }

    private String generateResetToken() {
        String seed = UUID.randomUUID() + ":" + Instant.now();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(seed.getBytes());
    }
}
