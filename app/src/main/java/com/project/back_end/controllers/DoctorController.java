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
@RequestMapping("${api.path}doctor")
public class DoctorController {
    private final DoctorService doctors;
    private final Service service;
    public DoctorController(DoctorService doctors, Service service) { this.doctors = doctors; this.service = service; }
    @GetMapping
    public Map<String, Object> getDoctor() { return Map.of("doctors", doctors.filterDoctor(null, null, null)); }
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public Map<String, Object> filter(@PathVariable String name, @PathVariable String time, @PathVariable String speciality) { return Map.of("doctors", doctors.filterDoctor(name, time, speciality)); }
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public Map<String, Object> getDoctorAvailability(@PathVariable String user, @PathVariable Long doctorId, @PathVariable LocalDate date, @PathVariable String token) {
        service.requireRole(token, user); return Map.of("availableTimes", doctors.getDoctorAvailability(doctorId, date));
    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> doctorLogin(@Valid @RequestBody Login login) { return doctors.validateDoctor(login); }
    @PostMapping("/{token}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> saveDoctor(@Valid @RequestBody Doctor doctor, @PathVariable String token) {
        service.requireRole(token, "admin"); doctors.saveDoctor(doctor, false); return Map.of("message", "Doctor created");
    }
    @PutMapping("/{token}")
    public Map<String, String> updateDoctor(@Valid @RequestBody Doctor doctor, @PathVariable String token) {
        service.requireRole(token, "admin"); doctors.saveDoctor(doctor, true); return Map.of("message", "Doctor updated");
    }
    @DeleteMapping("/{id}/{token}")
    public Map<String, String> deleteDoctor(@PathVariable Long id, @PathVariable String token) {
        service.requireRole(token, "admin"); doctors.deleteDoctor(id); return Map.of("message", "Doctor deleted");
    }
}
