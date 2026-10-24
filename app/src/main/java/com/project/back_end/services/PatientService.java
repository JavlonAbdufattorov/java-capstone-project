package com.project.back_end.services;
import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import com.project.back_end.DTO.AppointmentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@org.springframework.stereotype.Service
@Transactional
public class PatientService {
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final TokenService tokens;
    private final Service service;
    public PatientService(PatientRepository patients, AppointmentRepository appointments, TokenService tokens, Service service) {
        this.patients = patients; this.appointments = appointments; this.tokens = tokens; this.service = service;
    }
    public void createPatient(Patient patient) {
        if (!service.validatePatient(patient)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone already registered");
        patient.setId(null); patient.setPassword(service.hashPassword(patient.getPassword())); patients.save(patient);
    }
    public Patient getPatientDetails(String token) { return patients.findByEmail(tokens.extractEmail(token)); }
    public List<AppointmentDTO> getPatientAppointment(Long id, String role, String token, String condition, String name) {
        if (!DoctorService.omitted(condition) && !condition.equals("past") && !condition.equals("future")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown condition");
        String subject = tokens.extractEmail(token);
        if (role.equals("patient") && !getPatientDetails(token).getId().equals(id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient mismatch");
        if (!role.equals("patient") && !role.equals("doctor")) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported role");
        return appointments.findByPatientId(id).stream()
            .filter(a -> !role.equals("doctor") || a.getDoctor().getEmail().equals(subject))
            .filter(a -> DoctorService.omitted(condition) || a.getStatus() == (condition.equals("past") ? 1 : 0))
            .filter(a -> DoctorService.omitted(name) || a.getDoctor().getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(Appointment::getAppointmentTime)).map(AppointmentDTO::from).toList();
    }
}
