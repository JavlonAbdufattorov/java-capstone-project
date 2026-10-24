package com.project.back_end.repo;
import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    java.util.List<Appointment> findByPatientId(Long id);
    java.util.List<Appointment> findByDoctorIdAndAppointmentTimeBetween(Long id, java.time.LocalDateTime start, java.time.LocalDateTime end);
    void deleteAllByDoctorId(Long id);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Appointment a set a.status = :status where a.id = :id")
    int updateStatus(@org.springframework.data.repository.query.Param("status") int status, @org.springframework.data.repository.query.Param("id") Long id);
}
