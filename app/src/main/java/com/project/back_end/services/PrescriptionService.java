package com.project.back_end.services;
import com.project.back_end.models.*;
import com.project.back_end.repo.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@org.springframework.stereotype.Service
public class PrescriptionService {
    private final PrescriptionRepository prescriptions;
    private final AppointmentService appointments;
    public PrescriptionService(PrescriptionRepository prescriptions, AppointmentService appointments) {
        this.prescriptions = prescriptions; this.appointments = appointments;
    }
    public void savePrescription(Prescription prescription, String token) {
        Appointment a = appointments.find(prescription.getAppointmentId()); appointments.requireDoctor(a, token);
        if (!prescriptions.findByAppointmentId(a.getId()).isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription already exists");
        prescription.setId("appointment-" + a.getId()); prescription.setPatientName(a.getPatient().getName());
        prescriptions.insert(prescription);
        try { appointments.changeStatus(a.getId()); }
        catch (RuntimeException e) { prescriptions.deleteById(prescription.getId()); throw e; }
    }
    public List<Prescription> getPrescription(Long id, String token) {
        appointments.requireDoctor(appointments.find(id), token);
        return prescriptions.findByAppointmentId(id);
    }
}
