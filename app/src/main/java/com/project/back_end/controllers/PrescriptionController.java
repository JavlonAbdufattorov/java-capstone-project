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
@RequestMapping("${api.path}prescription")
public class PrescriptionController {
    private final PrescriptionService prescriptions;
    private final Service service;
    public PrescriptionController(PrescriptionService prescriptions, Service service) { this.prescriptions = prescriptions; this.service = service; }
    @PostMapping("/{token}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> savePrescription(@Valid @RequestBody Prescription prescription, @PathVariable String token) {
        service.requireRole(token, "doctor"); prescriptions.savePrescription(prescription, token); return Map.of("message", "Prescription created");
    }
    @GetMapping("/{appointmentId}/{token}")
    public Map<String, Object> getPrescription(@PathVariable Long appointmentId, @PathVariable String token) {
        service.requireRole(token, "doctor"); return Map.of("prescription", prescriptions.getPrescription(appointmentId, token));
    }
}
