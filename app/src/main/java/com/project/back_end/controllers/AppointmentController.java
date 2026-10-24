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
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService appointments;
    private final Service service;
    public AppointmentController(AppointmentService appointments, Service service) { this.appointments = appointments; this.service = service; }
    @GetMapping("/{date}/{patientName}/{token}")
    public Map<String, Object> getAppointments(@PathVariable LocalDate date, @PathVariable String patientName, @PathVariable String token) {
        service.requireRole(token, "doctor"); return Map.of("appointments", appointments.getAppointments(date, patientName, token));
    }
    @PostMapping("/{token}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> bookAppointment(@Valid @RequestBody Appointment appointment, @PathVariable String token) {
        service.requireRole(token, "patient"); appointments.bookAppointment(appointment, token, false); return Map.of("message", "Appointment booked");
    }
    @PutMapping("/{token}")
    public Map<String, String> updateAppointment(@Valid @RequestBody Appointment appointment, @PathVariable String token) {
        service.requireRole(token, "patient"); appointments.bookAppointment(appointment, token, true); return Map.of("message", "Appointment updated");
    }
    @DeleteMapping("/{id}/{token}")
    public Map<String, String> cancelAppointment(@PathVariable Long id, @PathVariable String token) {
        service.requireRole(token, "patient"); appointments.cancelAppointment(id, token); return Map.of("message", "Appointment cancelled");
    }
}
