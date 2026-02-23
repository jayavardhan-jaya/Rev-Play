package com.revature.revplaydemo.auth.security;

import com.revature.revplaydemo.auth.entity.UserEntity;
import com.revature.revplaydemo.auth.enums.UserRole;
import com.revature.revplaydemo.auth.exception.AuthUnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(UserEntity user) {
        return generateToken(user, TOKEN_TYPE_ACCESS, jwtProperties.getAccessTokenExpirationSeconds());
    }

    public String generateRefreshToken(UserEntity user) {
        return generateToken(user, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshTokenExpirationSeconds());
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception exception) {
            throw new AuthUnauthorizedException("Invalid or expired token");
        }
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(parseToken(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(parseToken(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public AuthenticatedUserPrincipal toPrincipal(String token) {
        Claims claims = parseToken(token);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        UserRole role = UserRole.from(claims.get(ROLE_CLAIM, String.class));
        return new AuthenticatedUserPrincipal(userId, username, role);
    }

    public Instant getExpiry(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return expiration == null ? null : expiration.toInstant();
    }

    private String generateToken(UserEntity user, String tokenType, long expirySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(secretKey())
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
