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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import tiameds.com.tiameds.entity.User;

@Repository
public interface VisitRepository extends JpaRepository<VisitEntity, Long> {

    @Query("SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab")
    List<VisitEntity> findAllByPatient_Labs(@Param("lab") Lab lab);

    @Query("SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient WHERE v.patient = :patientEntity")
    List<VisitEntity> findAllByPatient(@Param("patientEntity") PatientEntity patientEntity);

    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient WHERE v.patient.patientId = :patientId")
    List<VisitEntity> findByPatientId(@Param("patientId") Long patientId);


    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = :status AND v.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndStatus(@Param("labId") Long labId, @Param("status") String status, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate")
    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetween(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus = :visitStatus")
    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetweenAndVisitStatus(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("visitStatus") String visitStatus);

    // no filters
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate")
    Page<VisitEntity> findPaged(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    // status only
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus = :visitStatus",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus = :visitStatus")
    Page<VisitEntity> findPagedByStatus(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("visitStatus") String visitStatus, Pageable pageable);

    // search only
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                   "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern)",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                        "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern)")
    Page<VisitEntity> findPagedBySearch(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("searchPattern") String searchPattern, Pageable pageable);

    // search + status
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                   "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern) " +
                   "AND v.visitStatus = :visitStatus",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                        "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern) " +
                        "AND v.visitStatus = :visitStatus")
    Page<VisitEntity> findPagedBySearchAndStatus(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("searchPattern") String searchPattern, @Param("visitStatus") String visitStatus, Pageable pageable);


    @Modifying
    @Transactional
    @Query("UPDATE VisitEntity v SET v.visitStatus = :status WHERE v.visitId = :visitId")
    int updateVisitStatus(@Param("visitId") Long visitId, @Param("status") String status);


    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetweenAndVisitStatusIn(Lab lab, LocalDate startDate, LocalDate endDate, List<String> visitStatus);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending'")
    long countPendingVisitsByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending' AND v.createdAt BETWEEN :startDate AND :endDate")
    long countPendingVisitsByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.labs l WHERE l.id = :labId AND v.visitStatus = 'Pending'")
    long countPendingVisitsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.labs l WHERE l.id = :labId AND v.visitStatus = 'Pending' AND v.createdAt BETWEEN :startDate AND :endDate")
    long countPendingVisitsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId")
    long countAllVisitsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = :status")
    long countVisitsByLabIdAndStatus(@Param("labId") Long labId, @Param("status") String status);

    @Query("SELECT COUNT(v) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = :status AND v.createdAt BETWEEN :startDate AND :endDate")
    long countVisitsByLabIdAndStatusAndCreatedAtBetween(@Param("labId") Long labId, @Param("status") String status, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    // Distinct patients seen by a lab within a window — used by DashboardRollupService for daily_lab_stats.patient_count
    @Query("SELECT COUNT(DISTINCT v.patient.patientId) FROM VisitEntity v JOIN v.labs l WHERE l.id = :labId AND v.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctPatientsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
