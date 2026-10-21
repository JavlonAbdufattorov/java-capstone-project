package com.project.back_end.models;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PatientTest {

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
    void acceptsValidPatientWithoutAssignedId() {
        Patient patient = validPatient();
        assertThat(patient.getId()).isNull();
        assertThat(validator.validate(patient)).isEmpty();
    }

    @Test
    void requiresAllPatientDetails() {
        assertThat(validator.validate(new Patient()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "email", "password", "phone", "address");
    }

    @Test
    void returnsAssignedValues() {
        Patient patient = validPatient();
        patient.setId(7L);
        assertThat(patient.getId()).isEqualTo(7L);
        assertThat(patient.getName()).isEqualTo("Jane Smith");
        assertThat(patient.getEmail()).isEqualTo("jane@example.com");
        assertThat(patient.getPassword()).isEqualTo("secret123");
        assertThat(patient.getPhone()).isEqualTo("0123456789");
        assertThat(patient.getAddress()).isEqualTo("10 Main Street");
    }

    @ParameterizedTest
    @CsvSource({"name,2,false", "name,3,true", "name,100,true", "name,101,false",
            "password,5,false", "password,6,true", "address,255,true", "address,256,false"})
    void validatesLengthBoundaries(String property, int length, boolean valid) {
        Patient patient = validPatient();
        String value = "a".repeat(length);
        switch (property) {
            case "name" -> patient.setName(value);
            case "password" -> patient.setPassword(value);
            case "address" -> patient.setAddress(value);
            default -> throw new IllegalArgumentException(property);
        }
        assertValidity(patient, property, valid);
    }

    @Test
    void rejectsMalformedEmail() {
        Patient patient = validPatient();
        patient.setEmail("invalid-email");
        assertValidity(patient, "email", false);
    }

    @ParameterizedTest
    @CsvSource({"0123456789,true", "123456789,false", "12345678901,false",
            "123456789a,false", "+123456789,false", "123-456789,false"})
    void validatesPhone(String phone, boolean valid) {
        Patient patient = validPatient();
        patient.setPhone(phone);
        assertValidity(patient, "phone", valid);
    }

    private static Patient validPatient() {
        Patient patient = new Patient();
        patient.setName("Jane Smith");
        patient.setEmail("jane@example.com");
        patient.setPassword("secret123");
        patient.setPhone("0123456789");
        patient.setAddress("10 Main Street");
        return patient;
    }

    private static void assertValidity(Patient model, String property, boolean valid) {
        var violations = validator.validate(model);
        if (valid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly(property);
        }
    }
}
