package com.project.back_end.services;
import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import com.project.back_end.DTO.AppointmentDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final TokenService tokens;
    public AppointmentService(AppointmentRepository appointments, DoctorRepository doctors, PatientRepository patients, TokenService tokens) {
        this.appointments = appointments; this.doctors = doctors; this.patients = patients; this.tokens = tokens;
    }
    public Appointment find(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment ID required");
        return appointments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }
    public void requireDoctor(Appointment a, String token) {
        if (!a.getDoctor().getEmail().equals(tokens.extractEmail(token))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment belongs to another doctor");
    }
    private void requirePatient(Appointment a, String token) {
        if (!a.getPatient().getEmail().equals(tokens.extractEmail(token))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Appointment belongs to another patient");
    }
    public void bookAppointment(Appointment input, String token, boolean update) {
        Patient patient = patients.findByEmail(tokens.extractEmail(token));
        if (!Objects.equals(input.getPatient().getId(), patient.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient mismatch");
        Appointment target = input;
        if (update) {
            target = find(input.getId()); requirePatient(target, token);
            if (target.getStatus() != 0 || !target.getAppointmentTime().isAfter(LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment cannot be updated");
        }
        if (input.getDoctor().getId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor ID required");
        Doctor doctor = doctors.lockById(input.getDoctor().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        LocalDateTime start = input.getAppointmentTime();
        boolean scheduled = doctor.getAvailableTimes().stream().anyMatch(s -> LocalTime.parse(s.split("-")[0]).equals(start.toLocalTime()));
        boolean conflict = appointments.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start.minusHours(1), start.plusHours(1)).stream()
            .anyMatch(a -> (!update || !a.getId().equals(input.getId())) && start.isBefore(a.getEndTime()) && start.plusHours(1).isAfter(a.getAppointmentTime()));
        if (!scheduled || conflict) throw new ResponseStatusException(HttpStatus.CONFLICT, "Time slot is unavailable");
        if (!update) target.setId(null);
        target.setDoctor(doctor); target.setPatient(patient); target.setAppointmentTime(start); target.setStatus(0);
        appointments.save(target);
    }
    public void cancelAppointment(Long id, String token) {
        Appointment a = find(id); requirePatient(a, token);
        if (a.getStatus() != 0 || !a.getAppointmentTime().isAfter(LocalDateTime.now())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment cannot be cancelled");
        appointments.delete(a);
    }
    public List<AppointmentDTO> getAppointments(LocalDate date, String name, String token) {
        Doctor doctor = doctors.findByEmail(tokens.extractEmail(token));
        return appointments.findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay().minusNanos(1)).stream()
            .filter(a -> DoctorService.omitted(name) || a.getPatient().getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            .map(AppointmentDTO::from).toList();
    }
    public void changeStatus(Long id) { appointments.updateStatus(1, id); }
}
