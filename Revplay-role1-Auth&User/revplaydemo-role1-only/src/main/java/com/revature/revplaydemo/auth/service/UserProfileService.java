package com.revature.revplaydemo.auth.service;

import com.revature.revplaydemo.auth.dto.request.UpdateProfileRequest;
import com.revature.revplaydemo.auth.dto.response.UserProfileResponse;
import com.revature.revplaydemo.auth.entity.UserProfileEntity;
import com.revature.revplaydemo.auth.exception.AuthForbiddenException;
import com.revature.revplaydemo.auth.exception.AuthNotFoundException;
import com.revature.revplaydemo.auth.repository.UserProfileRepository;
import com.revature.revplaydemo.auth.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AdminAuditLogService adminAuditLogService;

    public UserProfileService(UserProfileRepository userProfileRepository, AdminAuditLogService adminAuditLogService) {
        this.userProfileRepository = userProfileRepository;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId, AuthenticatedUserPrincipal principal) {
        ensureSelfOrAdmin(userId, principal);
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthNotFoundException("Profile not found for user " + userId));
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request, AuthenticatedUserPrincipal principal) {
        ensureSelfOrAdmin(userId, principal);
        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthNotFoundException("Profile not found for user " + userId));

        profile.setFullName(request.fullName().trim());
        profile.setBio(request.bio());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setCountry(request.country());
        profile.setUpdatedAt(Instant.now());

        UserProfileEntity saved = userProfileRepository.save(profile);
        adminAuditLogService.log(
                principal.userId(),
                "PROFILE_UPDATE",
                "user_profiles",
                userId,
                "Profile details updated"
        );
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(UserProfileEntity profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getBio(),
                profile.getProfilePictureUrl(),
                profile.getCountry()
        );
    }

    private void ensureSelfOrAdmin(Long requestedUserId, AuthenticatedUserPrincipal principal) {
        boolean isAdmin = "ADMIN".equals(principal.role().name());
        if (!isAdmin && !requestedUserId.equals(principal.userId())) {
            throw new AuthForbiddenException("You can only access your own profile");
        }
    }
}
