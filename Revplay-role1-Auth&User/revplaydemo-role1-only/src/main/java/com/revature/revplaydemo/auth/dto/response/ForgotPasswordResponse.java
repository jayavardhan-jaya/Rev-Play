package com.revature.revplaydemo.auth.dto.response;

import java.time.Instant;

public record ForgotPasswordResponse(
        String message,
        String resetToken,
        Instant expiresAt
) {
}
