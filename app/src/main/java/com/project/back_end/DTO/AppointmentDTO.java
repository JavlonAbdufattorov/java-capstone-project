package com.project.back_end.DTO;
import com.project.back_end.models.Appointment;
import java.time.*;
public record AppointmentDTO(Long id, Long doctorId, String doctorName, Long patientId,
        String patientName, String patientEmail, String patientPhone, String patientAddress,
        LocalDateTime appointmentTime, int status) {
    public static AppointmentDTO from(Appointment a) {
        return new AppointmentDTO(a.getId(), a.getDoctor().getId(), a.getDoctor().getName(),
            a.getPatient().getId(), a.getPatient().getName(), a.getPatient().getEmail(),
            a.getPatient().getPhone(), a.getPatient().getAddress(), a.getAppointmentTime(), a.getStatus());
    }
    public LocalDate getAppointmentDate() { return appointmentTime.toLocalDate(); }
    public LocalTime getAppointmentTimeOnly() { return appointmentTime.toLocalTime(); }
    public LocalDateTime getEndTime() { return appointmentTime.plusHours(1); }
}
