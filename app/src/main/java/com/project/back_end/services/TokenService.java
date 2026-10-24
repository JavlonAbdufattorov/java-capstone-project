package com.project.back_end.services;
import com.project.back_end.repo.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
@Component
public class TokenService {
    private final AdminRepository admins;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final SecretKey key;
    public TokenService(AdminRepository admins, DoctorRepository doctors, PatientRepository patients,
                        @Value("${jwt.secret}") String secret) {
        this.admins = admins; this.doctors = doctors; this.patients = patients;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String generateToken(String subject, String role) {
        Instant now = Instant.now();
        return Jwts.builder().subject(subject).claim("role", role).issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(7, ChronoUnit.DAYS))).signWith(key).compact();
    }
    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
    public String extractEmail(String token) { return claims(token).getSubject(); }
    public boolean validateToken(String token, String role) {
        try {
            Claims claims = claims(token);
            if (!role.equals(claims.get("role", String.class))) return false;
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) return false;
            return switch (role) {
                case "admin" -> admins.findByUsername(subject) != null;
                case "doctor" -> doctors.findByEmail(subject) != null;
                case "patient" -> patients.findByEmail(subject) != null;
                default -> false;
            };
        } catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
