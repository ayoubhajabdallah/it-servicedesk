package dev.ayoub.servicedesk.service;

import dev.ayoub.servicedesk.domain.User;
import dev.ayoub.servicedesk.domain.UserRole;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "local-demo-only-0123456789abcdef";
    private final JwtService jwtService = new JwtService(SECRET);
    private final User user = User.builder().email("employee@example.com").role(UserRole.EMPLOYEE).build();

    @Test
    void generatesAndParsesHs256TokenWithDocumentedSecret() {
        assertEquals(32, SECRET.getBytes(StandardCharsets.UTF_8).length);
        String token = jwtService.generateToken(user);
        assertEquals(user.getEmail(), jwtService.extractEmail(token));
        var parsed = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token);
        assertEquals("HS256", parsed.getHeader().getAlgorithm());
        assertEquals("EMPLOYEE", parsed.getPayload().get("role"));
        assertEquals(7200, (parsed.getPayload().getExpiration().getTime()
                - parsed.getPayload().getIssuedAt().getTime()) / 1000);
    }

    @Test
    void rejectsShortSigningKey() {
        JwtService shortKeyService = new JwtService("your-secure-development-secret");
        assertThrows(WeakKeyException.class, () -> shortKeyService.generateToken(user));
    }

    @Test
    void rejectsTamperedSignature() {
        String token = jwtService.generateToken(user);
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        assertThrows(JwtException.class, () -> jwtService.extractEmail(tampered));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(JwtException.class, () -> jwtService.extractEmail("not-a-jwt"));
    }

    @Test
    void rejectsExpiredToken() {
        String token = Jwts.builder().subject(user.getEmail())
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThrows(JwtException.class, () -> jwtService.extractEmail(token));
    }
}
