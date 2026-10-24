package com.project.back_end.services;
import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import com.project.back_end.DTO.Login;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@org.springframework.stereotype.Service
@Transactional
public class DoctorService {
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final Service service;
    public DoctorService(DoctorRepository doctors, AppointmentRepository appointments, Service service) {
        this.doctors = doctors; this.appointments = appointments; this.service = service;
    }
    public static boolean omitted(String s) { return s == null || s.isBlank() || s.equalsIgnoreCase("null") || s.equalsIgnoreCase("all"); }
    public List<Doctor> filterDoctor(String name, String time, String specialty) {
        if (!omitted(time) && !time.equalsIgnoreCase("AM") && !time.equalsIgnoreCase("PM"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time must be AM or PM");
        return doctors.findAll().stream()
            .filter(d -> omitted(name) || d.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            .filter(d -> omitted(specialty) || d.getSpecialty().equalsIgnoreCase(specialty))
            .peek(d -> d.getAvailableTimes().size())
            .filter(d -> omitted(time) || d.getAvailableTimes().stream().anyMatch(t -> (LocalTime.parse(t.split("-")[0]).getHour() < 12) == time.equalsIgnoreCase("AM")))
            .toList();
    }
    public List<String> getDoctorAvailability(Long id, LocalDate date) {
        Doctor doctor = doctors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        List<Appointment> booked = appointments.findByDoctorIdAndAppointmentTimeBetween(id, date.atStartOfDay().minusHours(1), date.plusDays(1).atStartOfDay());
        return doctor.getAvailableTimes().stream().filter(slot -> {
            LocalDateTime start = date.atTime(LocalTime.parse(slot.split("-")[0]));
            return start.isAfter(LocalDateTime.now()) && booked.stream().noneMatch(a -> start.isBefore(a.getEndTime()) && start.plusHours(1).isAfter(a.getAppointmentTime()));
        }).toList();
    }
    public void saveDoctor(Doctor doctor, boolean update) {
        if (update && (doctor.getId() == null || !doctors.existsById(doctor.getId())))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
        Doctor duplicate = doctors.findByEmail(doctor.getEmail());
        if (duplicate != null && (!update || !duplicate.getId().equals(doctor.getId())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor email already exists");
        if (doctor.getAvailableTimes() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Availability is required");
        for (String slot : doctor.getAvailableTimes()) {
            try {
                String[] parts = slot.split("-");
                if (parts.length != 2 || !LocalTime.parse(parts[0]).plusHours(1).equals(LocalTime.parse(parts[1]))) throw new IllegalArgumentException();
            } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use one-hour slots: HH:mm-HH:mm"); }
        }
        if (!update) doctor.setId(null);
        doctor.setPassword(service.hashPassword(doctor.getPassword()));
        doctors.save(doctor);
    }
    public void deleteDoctor(Long id) {
        if (!doctors.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
        appointments.deleteAllByDoctorId(id); doctors.deleteById(id);
    }
    public ResponseEntity<Map<String, Object>> validateDoctor(Login login) {
        Doctor d = doctors.findByEmail(login.getEmail());
        return service.login(login.getEmail(), login.getPassword(), d == null ? null : d.getPassword(), "doctor");
    }
}
