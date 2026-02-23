package com.revature.revplaydemo.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService {

    private final Map<String, Instant> revokedTokensByExpiry = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiry) {
        if (token == null || token.isBlank() || expiry == null) {
            return;
        }
        revokedTokensByExpiry.put(token, expiry);
        cleanupExpired();
    }

    public boolean isRevoked(String token) {
        Instant expiry = revokedTokensByExpiry.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            revokedTokensByExpiry.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        revokedTokensByExpiry.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
