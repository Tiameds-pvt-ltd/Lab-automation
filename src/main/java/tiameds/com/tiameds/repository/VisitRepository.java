package tiameds.com.tiameds.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRepository extends JpaRepository<VisitEntity, Long> {

    List<VisitEntity> findAllByPatient_Labs(Lab lab);

    List<VisitEntity> findAllByPatient(PatientEntity patientEntity);

    @Query("SELECT v FROM VisitEntity v WHERE v.patient.patientId = :patientId")
    List<VisitEntity> findByPatientId(@Param("patientId") Long patientId);


    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = :status AND v.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndStatus(@Param("labId") Long labId, @Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetween(Lab lab, LocalDate startDate, LocalDate endDate);

    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetweenAndVisitStatus(Lab lab, LocalDate startDate, LocalDate endDate, String visitStatus);


    @Modifying
    @Transactional
    @Query("UPDATE VisitEntity v SET v.visitStatus = :status WHERE v.visitId = :visitId")
    int updateVisitStatus(@Param("visitId") Long visitId, @Param("status") String status);
}
