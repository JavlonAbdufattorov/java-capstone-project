package com.project.back_end.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoctorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void returnsAssignedValues() {
        Doctor doctor = validDoctor();

        assertThat(doctor.getId()).isEqualTo(1L);
        assertThat(doctor.getName()).isEqualTo("Doctor Smith");
        assertThat(doctor.getSpecialty()).isEqualTo("Cardiology");
        assertThat(doctor.getEmail()).isEqualTo("smith@example.com");
        assertThat(doctor.getPassword()).isEqualTo("secret123");
        assertThat(doctor.getPhone()).isEqualTo("1234567890");
        assertThat(doctor.getAvailableTimes())
                .containsExactly("09:00-10:00", "10:00-11:00");
    }

    @Test
    void acceptsValidDoctor() {
        assertThat(validator.validate(validDoctor())).isEmpty();
    }

    @Test
    void requiresNameSpecialtyPasswordAndPhone() {
        assertThat(validator.validate(new Doctor()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "specialty", "password", "phone");
    }

    @ParameterizedTest
    @CsvSource({"2,false", "3,true", "100,true", "101,false"})
    void validatesNameLength(int length, boolean valid) {
        Doctor doctor = validDoctor();
        doctor.setName("a".repeat(length));

        assertValidity(doctor, "name", valid);
    }

    @ParameterizedTest
    @CsvSource({"2,false", "3,true", "50,true", "51,false"})
    void validatesSpecialtyLength(int length, boolean valid) {
        Doctor doctor = validDoctor();
        doctor.setSpecialty("a".repeat(length));

        assertValidity(doctor, "specialty", valid);
    }

    @ParameterizedTest
    @CsvSource({"5,false", "6,true"})
    void validatesPasswordLength(int length, boolean valid) {
        Doctor doctor = validDoctor();
        doctor.setPassword("a".repeat(length));

        assertValidity(doctor, "password", valid);
    }

    @Test
    void rejectsMalformedEmail() {
        Doctor doctor = validDoctor();
        doctor.setEmail("invalid-email");

        assertValidity(doctor, "email", false);
    }

    @ParameterizedTest
    @CsvSource({"123456789,false", "1234567890,true",
            "12345678901,false", "123456789a,false", "+123456789,false"})
    void validatesPhone(String phone, boolean valid) {
        Doctor doctor = validDoctor();
        doctor.setPhone(phone);

        assertValidity(doctor, "phone", valid);
    }

    @Test
    void hidesPasswordInJsonButAcceptsItOnInput() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Doctor doctor = validDoctor();

        var json = mapper.readTree(mapper.writeValueAsString(doctor));
        assertThat(json.has("password")).isFalse();
        assertThat(json.get("email").asText()).isEqualTo("smith@example.com");

        Doctor deserialized = mapper.readValue(
                "{\"password\":\"incomingSecret\"}", Doctor.class);
        assertThat(deserialized.getPassword()).isEqualTo("incomingSecret");
    }

    private static void assertValidity(Doctor doctor, String property, boolean valid) {
        var violations = validator.validate(doctor);
        if (valid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly(property);
        }
    }

    private static Doctor validDoctor() {
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Doctor Smith");
        doctor.setSpecialty("Cardiology");
        doctor.setEmail("smith@example.com");
        doctor.setPassword("secret123");
        doctor.setPhone("1234567890");
        doctor.setAvailableTimes(List.of("09:00-10:00", "10:00-11:00"));
        return doctor;
    }
}
