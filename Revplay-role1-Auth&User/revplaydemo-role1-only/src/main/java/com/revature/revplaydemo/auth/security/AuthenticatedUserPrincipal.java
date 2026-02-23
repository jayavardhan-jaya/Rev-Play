package com.revature.revplaydemo.auth.security;

import com.revature.revplaydemo.auth.enums.UserRole;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        UserRole role
) {
}
