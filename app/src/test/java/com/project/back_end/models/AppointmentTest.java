package com.project.back_end.models;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentTest {

    @Test
    void derivesDisplayValuesAcrossMidnight() {
        Appointment appointment = new Appointment();
        appointment.setAppointmentTime(LocalDateTime.of(2030, 12, 31, 23, 30));

        assertThat(appointment.getEndTime()).isEqualTo(LocalDateTime.of(2031, 1, 1, 0, 30));
        assertThat(appointment.getAppointmentDate()).isEqualTo(LocalDate.of(2030, 12, 31));
        assertThat(appointment.getAppointmentTimeOnly()).isEqualTo(LocalTime.of(23, 30));
    }

    @Test
    void handlesMissingTimeWithoutThrowing() {
        Appointment appointment = new Appointment();

        assertThat(appointment.getEndTime()).isNull();
        assertThat(appointment.getAppointmentDate()).isNull();
        assertThat(appointment.getAppointmentTimeOnly()).isNull();
    }

    @Test
    void requiresParticipantsAndAStrictlyFutureTime() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
        try (var factory = Validation.byDefaultProvider().configure()
                .clockProvider(() -> clock).buildValidatorFactory()) {
            var validator = factory.getValidator();
            Appointment appointment = new Appointment();

            assertThat(validator.validate(appointment))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("doctor", "patient", "appointmentTime");

            appointment.setDoctor(new Doctor());
            appointment.setPatient(new Patient());
            LocalDateTime now = LocalDateTime.now(clock);
            for (LocalDateTime invalidTime : new LocalDateTime[]{now.minusMinutes(1), now}) {
                appointment.setAppointmentTime(invalidTime);
                assertThat(validator.validate(appointment))
                        .extracting(violation -> violation.getPropertyPath().toString())
                        .containsExactly("appointmentTime");
            }

            appointment.setAppointmentTime(now.plusMinutes(1));
            assertThat(validator.validate(appointment)).isEmpty();
        }
    }
}
