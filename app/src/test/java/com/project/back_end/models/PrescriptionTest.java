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

class PrescriptionTest {

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
    void constructorInitializesPrescriptionWithoutAssignedId() {
        Prescription prescription = validPrescription();
        assertThat(prescription.getId()).isNull();
        assertThat(prescription.getPatientName()).isEqualTo("Jane Smith");
        assertThat(prescription.getMedication()).isEqualTo("Medication A");
        assertThat(prescription.getDosage()).isEqualTo("Prescribed dosage");
        assertThat(prescription.getDoctorNotes()).isEqualTo("Follow instructions");
        assertThat(prescription.getAppointmentId()).isEqualTo(42L);
        assertThat(validator.validate(prescription)).isEmpty();
    }

    @Test
    void allowsUpdatingAllFields() {
        Prescription prescription = validPrescription();
        prescription.setId("prescription-1");
        prescription.setPatientName("John Smith");
        prescription.setMedication("Medication B");
        prescription.setDosage("Updated dosage");
        prescription.setDoctorNotes("Updated notes");
        prescription.setAppointmentId(43L);
        assertThat(prescription.getId()).isEqualTo("prescription-1");
        assertThat(prescription.getPatientName()).isEqualTo("John Smith");
        assertThat(prescription.getMedication()).isEqualTo("Medication B");
        assertThat(prescription.getDosage()).isEqualTo("Updated dosage");
        assertThat(prescription.getDoctorNotes()).isEqualTo("Updated notes");
        assertThat(prescription.getAppointmentId()).isEqualTo(43L);
        assertThat(validator.validate(prescription)).isEmpty();
    }

    @Test
    void requiresPatientAppointmentMedicationAndDosage() {
        assertThat(validator.validate(new Prescription()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("patientName", "appointmentId", "medication", "dosage");
    }

    @Test
    void allowsMissingDoctorNotes() {
        Prescription prescription = validPrescription();
        prescription.setDoctorNotes(null);
        assertThat(validator.validate(prescription)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"patientName,2,false", "patientName,3,true", "patientName,100,true",
            "patientName,101,false", "medication,2,false", "medication,3,true",
            "medication,100,true", "medication,101,false", "doctorNotes,0,true",
            "doctorNotes,200,true", "doctorNotes,201,false"})
    void validatesLengthBoundaries(String property, int length, boolean valid) {
        Prescription prescription = validPrescription();
        String value = "a".repeat(length);
        switch (property) {
            case "patientName" -> prescription.setPatientName(value);
            case "medication" -> prescription.setMedication(value);
            case "doctorNotes" -> prescription.setDoctorNotes(value);
            default -> throw new IllegalArgumentException(property);
        }
        assertValidity(prescription, property, valid);
    }

    private static Prescription validPrescription() {
        return new Prescription("Jane Smith", "Medication A", "Prescribed dosage",
                "Follow instructions", 42L);
    }

    private static void assertValidity(Prescription model, String property, boolean valid) {
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
