package com.project.back_end.services;
import com.project.back_end.models.*;
import com.project.back_end.DTO.Login;
import com.project.back_end.repo.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Map;
@org.springframework.stereotype.Service
public class Service {
    private final TokenService tokens;
    private final AdminRepository admins;
    private final PatientRepository patients;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    public Service(TokenService tokens, AdminRepository admins, PatientRepository patients) {
        this.tokens = tokens; this.admins = admins; this.patients = patients;
    }
    public Map<String, String> validateToken(String token, String role) {
        return tokens.validateToken(token, role) ? Map.of() : Map.of("message", "Invalid or expired token");
    }
    public void requireRole(String token, String role) {
        if (!validateToken(token, role).isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
    public String hashPassword(String password) { return passwords.encode(password); }
    public ResponseEntity<Map<String, Object>> login(String subject, String password, String stored, String role) {
        if (stored == null || password == null || !passwords.matches(password, stored))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        return ResponseEntity.ok(Map.of("token", tokens.generateToken(subject, role)));
    }
    public ResponseEntity<Map<String, Object>> validateAdmin(Admin input) {
        Admin admin = admins.findByUsername(input.getUsername());
        return login(input.getUsername(), input.getPassword(), admin == null ? null : admin.getPassword(), "admin");
    }
    public ResponseEntity<Map<String, Object>> validatePatientLogin(Login input) {
        Patient patient = patients.findByEmail(input.getEmail());
        return login(input.getEmail(), input.getPassword(), patient == null ? null : patient.getPassword(), "patient");
    }
    public boolean validatePatient(Patient patient) {
        return patients.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }
}
