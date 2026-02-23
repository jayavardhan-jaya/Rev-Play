package com.revature.revplaydemo.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Boolean isActive
) {
}
