package com.project.back_end.services;

import com.project.back_end.models.*;
import com.project.back_end.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TokenService tokens = mock(TokenService.class);
    private final AppointmentService service = new AppointmentService(appointments, doctors, patients, tokens);
    private Appointment input() {
        Patient patient = new Patient(); patient.setId(1L); patient.setEmail("patient@example.com");
        Doctor doctor = new Doctor(); doctor.setId(2L); doctor.setAvailableTimes(List.of("09:00-10:00"));
        when(tokens.extractEmail("token")).thenReturn(patient.getEmail());
        when(patients.findByEmail(patient.getEmail())).thenReturn(patient);
        when(doctors.lockById(2L)).thenReturn(Optional.of(doctor));
        Appointment a = new Appointment(); a.setPatient(patient); a.setDoctor(doctor);
        a.setAppointmentTime(LocalDate.now().plusDays(1).atTime(9, 0));
        return a;
    }
    @Test void bookingUsesAuthenticatedPatientAndResetsClientStatusAndId() {
        Appointment a = input(); a.setId(999L); a.setStatus(1);
        service.bookAppointment(a, "token", false);
        assertThat(a.getId()).isNull(); assertThat(a.getStatus()).isZero();
        verify(appointments).save(a);
    }
    @Test void rejectsOverlappingSlot() {
        Appointment a = input(); Appointment existing = new Appointment(); existing.setId(3L);
        existing.setAppointmentTime(a.getAppointmentTime().minusMinutes(30));
        when(appointments.findByDoctorIdAndAppointmentTimeBetween(anyLong(), any(), any())).thenReturn(List.of(existing));
        assertThatThrownBy(() -> service.bookAppointment(a, "token", false))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verify(appointments, never()).save(any());
    }
    @Test void cannotCancelAnotherPatientsAppointment() {
        Appointment a = input(); a.setId(3L); when(appointments.findById(3L)).thenReturn(Optional.of(a));
        when(tokens.extractEmail("other")).thenReturn("other@example.com");
        assertThatThrownBy(() -> service.cancelAppointment(3L, "other"))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verify(appointments, never()).delete(any());
    }
}
