package peps.peps_back.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * @author Santiago Alexander RODRIGUEZ TRIANA
 * @description JWT utility — generates and validates signed JWT tokens stored as HttpOnly cookies.
 * Token claims:
 *   sub         = login (username)
 *   userId      = database user ID (long)
 *   role        = user role string ("ROLE_ADMIN", "ROLE_SOIGNEUR", "ROLE_CHERCHEUR")
 *   permission  = LEGACY claim ("editor" | "viewer") maintained temporarily for backward compatibility
 *   iat         = issued-at
 *   exp         = expiry (LOGIN_EXPIRY_MS after iat, or Instant.EPOCH for logout tokens)
 * Secret: read from environment variable JWT_SECRET.
 * Falls back to a hardcoded development secret if the env var is absent.
 * IMPORTANT: set JWT_SECRET to a strong random value (>= 256 bits) in production.
 */
public class JwtUtil {

    /**
     * 8 hours in milliseconds — token lifetime for a normal login session.
     * TODO: Change to 30 minutes (30 * 60 * 1000L) when refactoring security/ and controllers/.
     */
    private static final long LOGIN_EXPIRY_MS = 8 * 60 * 60 * 1000L;

    /** Cookie name used throughout the application. */
    public static final String COOKIE_NAME = "jwt";

    // -------------------------------------------------------------------------
    // Secret key — resolved once at class-load time
    // -------------------------------------------------------------------------
    private static final SecretKey SECRET_KEY = buildKey();

    private static SecretKey buildKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            // Development fallback — NOT secure for production
            secret = "PEPs_DEV_SECRET_CHANGE_ME_IN_PROD_32chars!!";
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a valid JWT token using the new RBAC role model.
     * Automatically populates the legacy {@code permission} claim so existing controllers
     * and {@link JwtFilter} continue working until Phase 3 refactoring.
     *
     * @param userId database user ID
     * @param login  username
     * @param role   RBAC role (e.g., "ROLE_ADMIN", "ROLE_SOIGNEUR", "ROLE_CHERCHEUR")
     * @return signed JWT string
     */
    public static String generateToken(long userId, String login, String role) {
        String legacyPermission = resolveLegacyPermission(role);
        return generateToken(userId, login, role, legacyPermission);
    }

    /**
     * Generates a valid JWT token for the given user.
     *
     * @deprecated Legacy overload kept temporarily for backward compatibility with unrefactored controllers.
     *             TODO: Remove the {@code permission} claim once all controllers check {@code role}.
     */
    @Deprecated
    public static String generateToken(long userId, String login, String role, String permission) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(login)
                .claim("userId", userId)
                .claim("role", role)
                .claim("permission", permission)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(LOGIN_EXPIRY_MS)))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates an immediately-expired JWT token.
     * Used by the logout endpoint to actively invalidate any in-flight requests
     * that arrive before the browser processes the Max-Age=0 cookie directive.
     */
    public static String generateExpiredToken(String login) {
        Instant past = Instant.EPOCH; // 1970-01-01 — always expired
        return Jwts.builder()
                .setSubject(login)
                .setIssuedAt(Date.from(past))
                .setExpiration(Date.from(past))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    /**
     * Parses and validates the given token.
     *
     * @param token the raw JWT string (from the cookie value)
     * @return the Claims if the token is valid and not expired, or {@code null}
     *         if the token is missing, malformed, expired, or has a bad signature
     */
    public static Claims validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            // Expired, invalid signature, malformed — all treated the same
            return null;
        }
    }

    /**
     * Temporary adapter translating modern RBAC roles to the legacy permission claim.
     * TODO: Delete this helper when removing legacy permission support.
     */
    private static String resolveLegacyPermission(String role) {
        if ("ROLE_ADMIN".equals(role) || "ROLE_SOIGNEUR".equals(role)) {
            return "editor";
        }
        return "viewer";
    }
}