package com.revature.revplaydemo.auth.service;

import com.revature.revplaydemo.auth.dto.request.UpdateUserRoleRequest;
import com.revature.revplaydemo.auth.dto.request.UpdateUserStatusRequest;
import com.revature.revplaydemo.auth.dto.response.SimpleMessageResponse;
import com.revature.revplaydemo.auth.entity.UserEntity;
import com.revature.revplaydemo.auth.enums.UserRole;
import com.revature.revplaydemo.auth.exception.AuthNotFoundException;
import com.revature.revplaydemo.auth.repository.UserRepository;
import com.revature.revplaydemo.auth.security.AuthenticatedUserPrincipal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountAdminService {

    private final UserRepository userRepository;
    private final AdminAuditLogService adminAuditLogService;

    public UserAccountAdminService(UserRepository userRepository, AdminAuditLogService adminAuditLogService) {
        this.userRepository = userRepository;
        this.adminAuditLogService = adminAuditLogService;
    }

    @Transactional
    public SimpleMessageResponse updateStatus(Long userId, UpdateUserStatusRequest request, AuthenticatedUserPrincipal admin) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        user.setIsActive(request.isActive());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        adminAuditLogService.log(
                admin.userId(),
                "ACCOUNT_STATUS_UPDATE",
                "users",
                userId,
                "is_active set to " + request.isActive()
        );
        return new SimpleMessageResponse("Account status updated");
    }

    @Transactional
    public SimpleMessageResponse updateRole(Long userId, UpdateUserRoleRequest request, AuthenticatedUserPrincipal admin) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        UserRole oldRole = user.getRole();
        UserRole newRole = UserRole.from(request.role());
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        adminAuditLogService.log(
                admin.userId(),
                "ROLE_CHANGE",
                "users",
                userId,
                "Role changed from " + oldRole.name() + " to " + newRole.name()
        );
        return new SimpleMessageResponse("User role updated");
    }
}
