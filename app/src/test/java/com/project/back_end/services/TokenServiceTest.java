package com.project.back_end.services;

import com.project.back_end.repo.*;
import com.project.back_end.models.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenServiceTest {
    private static final String SECRET = "test-secret-at-least-thirty-two-bytes-long";
    private final AdminRepository admins = mock(AdminRepository.class);
    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TokenService tokens = new TokenService(admins, doctors, patients, SECRET);
    @Test void tokenCannotCrossRolesEvenWhenIdentitiesMatch() {
        when(patients.findByEmail("same@example.com")).thenReturn(new Patient());
        when(doctors.findByEmail("same@example.com")).thenReturn(new Doctor());
        String token = tokens.generateToken("same@example.com", "patient");
        assertThat(tokens.validateToken(token, "patient")).isTrue();
        assertThat(tokens.validateToken(token, "doctor")).isFalse();
        assertThat(tokens.validateToken(token, "admin")).isFalse();
    }
    @Test void rejectsMalformedExpiredAndDeletedUserTokens() {
        assertThat(tokens.validateToken("garbage", "admin")).isFalse();
        assertThat(tokens.validateToken(null, "admin")).isFalse();
        assertThat(tokens.validateToken(tokens.generateToken("deleted", "admin"), "admin")).isFalse();
        String expired = Jwts.builder().subject("admin").claim("role", "admin")
            .expiration(new Date(0)).signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(tokens.validateToken(expired, "admin")).isFalse();
    }
}
