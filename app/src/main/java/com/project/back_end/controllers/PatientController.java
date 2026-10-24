package com.project.back_end.controllers;
import com.project.back_end.services.*;
import com.project.back_end.services.Service;
import com.project.back_end.models.*;
import com.project.back_end.DTO.Login;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/patient")
public class PatientController {
    private final PatientService patients;
    private final Service service;
    public PatientController(PatientService patients, Service service) { this.patients = patients; this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createPatient(@Valid @RequestBody Patient patient) { patients.createPatient(patient); return Map.of("message", "Patient created"); }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody Login login) { return service.validatePatientLogin(login); }
    @GetMapping("/{token}")
    public Map<String, Object> getPatient(@PathVariable String token) { service.requireRole(token, "patient"); return Map.of("patient", patients.getPatientDetails(token)); }
    @GetMapping("/{id}/{user}/{token}")
    public Map<String, Object> getPatientAppointment(@PathVariable Long id, @PathVariable String user, @PathVariable String token) {
        service.requireRole(token, user); return Map.of("appointments", patients.getPatientAppointment(id, user, token, null, null));
    }
    @GetMapping("/filter/{condition}/{name}/{token}")
    public Map<String, Object> filterPatientAppointment(@PathVariable String condition, @PathVariable String name, @PathVariable String token) {
        service.requireRole(token, "patient"); return Map.of("appointments", patients.getPatientAppointment(patients.getPatientDetails(token).getId(), "patient", token, condition, name));
    }
}
