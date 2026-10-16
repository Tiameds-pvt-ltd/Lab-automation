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

    /**
     * Stores the test name and price on the patient_visit_tests rows of a newly created visit,
     * so later changes to the master tests table don't alter this visit's history.
     * Must run in the same transaction that created the visit; the pending join-table rows are
     * flushed first. Only fills NULL columns, so existing snapshots are never overwritten.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE patient_visit_tests pvt SET test_name = t.name, price = t.price " +
            "FROM tests t, patient_visits v " +
            "WHERE pvt.test_id = t.test_id AND pvt.visit_id = v.visit_id " +
            "AND v.visit_code = :visitCode " +
            "AND (pvt.test_name IS NULL OR pvt.price IS NULL)", nativeQuery = true)
    int snapshotTestNameAndPrice(@Param("visitCode") String visitCode);

    /** Same as {@link #snapshotTestNameAndPrice(String)} but for a visit identified by id. */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE patient_visit_tests pvt SET test_name = t.name, price = t.price " +
            "FROM tests t " +
            "WHERE pvt.test_id = t.test_id AND pvt.visit_id = :visitId " +
            "AND (pvt.test_name IS NULL OR pvt.price IS NULL)", nativeQuery = true)
    int snapshotTestNameAndPriceByVisitId(@Param("visitId") Long visitId);

    /** Name/price currently saved on a visit's tests (read before an edit replaces the rows). */
    interface VisitTestSnapshot {
        Long getTestId();

        String getTestName();

        java.math.BigDecimal getPrice();
    }

    @Query(value = "SELECT test_id AS \"testId\", test_name AS \"testName\", price AS \"price\" " +
            "FROM patient_visit_tests WHERE visit_id = :visitId " +
            "AND (test_name IS NOT NULL OR price IS NOT NULL)", nativeQuery = true)
    List<VisitTestSnapshot> findTestSnapshotsByVisitId(@Param("visitId") Long visitId);

    /** Puts a previously saved name/price back on a re-inserted row; never overwrites existing values. */
    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE patient_visit_tests SET " +
            "test_name = COALESCE(test_name, CAST(:testName AS varchar)), " +
            "price = COALESCE(price, CAST(:price AS numeric)) " +
            "WHERE visit_id = :visitId AND test_id = :testId", nativeQuery = true)
    int restoreTestSnapshot(@Param("visitId") Long visitId, @Param("testId") Long testId,
                            @Param("testName") String testName, @Param("price") java.math.BigDecimal price);

    @Query(value = "SELECT visit_id FROM patient_visits WHERE patient_id = :patientId", nativeQuery = true)
    List<Long> findVisitIdsByPatientId(@Param("patientId") Long patientId);

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
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l LEFT JOIN FETCH v.billing LEFT JOIN FETCH v.doctor WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus = :visitStatus",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus = :visitStatus")
    Page<VisitEntity> findPagedByStatus(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("visitStatus") String visitStatus, Pageable pageable);

    // search only
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                   "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern)",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                        "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern)")
    Page<VisitEntity> findPagedBySearch(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("searchPattern") String searchPattern, Pageable pageable);

    // search + status
    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l LEFT JOIN FETCH v.billing LEFT JOIN FETCH v.doctor WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                   "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern) " +
                   "AND v.visitStatus = :visitStatus",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate " +
                        "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern) " +
                        "AND v.visitStatus = :visitStatus")
    Page<VisitEntity> findPagedBySearchAndStatus(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("searchPattern") String searchPattern, @Param("visitStatus") String visitStatus, Pageable pageable);


    @Modifying
    @Transactional
    @Query("UPDATE VisitEntity v SET v.visitStatus = :status WHERE v.visitId = :visitId")
    int updateVisitStatus(@Param("visitId") Long visitId, @Param("status") String status);


    List<VisitEntity> findAllByPatient_LabsAndVisitDateBetweenAndVisitStatusIn(Lab lab, LocalDate startDate, LocalDate endDate, List<String> visitStatus);

    @Query(value = "SELECT DISTINCT v FROM VisitEntity v JOIN FETCH v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus IN :visitStatuses",
           countQuery = "SELECT COUNT(DISTINCT v.visitId) FROM VisitEntity v JOIN v.patient p JOIN p.labs l WHERE l = :lab AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus IN :visitStatuses")
    Page<VisitEntity> findPagedByStatusIn(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("visitStatuses") List<String> visitStatuses, Pageable pageable);

    @Query(value = "SELECT v FROM VisitEntity v JOIN FETCH v.patient p " +
                   "WHERE v.patient IN (SELECT p2 FROM PatientEntity p2 JOIN p2.labs l WHERE l = :lab) " +
                   "AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus IN :visitStatuses " +
                   "AND (LOWER(p.firstName) LIKE :searchPattern OR LOWER(p.lastName) LIKE :searchPattern " +
                   "OR LOWER(p.phone) LIKE :searchPattern OR LOWER(p.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern)",
           countQuery = "SELECT COUNT(v) FROM VisitEntity v " +
                        "WHERE v.patient IN (SELECT p2 FROM PatientEntity p2 JOIN p2.labs l WHERE l = :lab) " +
                        "AND v.visitDate BETWEEN :startDate AND :endDate AND v.visitStatus IN :visitStatuses " +
                        "AND (LOWER(v.patient.firstName) LIKE :searchPattern OR LOWER(v.patient.lastName) LIKE :searchPattern " +
                        "OR LOWER(v.patient.phone) LIKE :searchPattern OR LOWER(v.patient.patientCode) LIKE :searchPattern OR LOWER(v.visitCode) LIKE :searchPattern)")
    Page<VisitEntity> findPagedByStatusInAndSearch(@Param("lab") Lab lab, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("visitStatuses") List<String> visitStatuses, @Param("searchPattern") String searchPattern, Pageable pageable);

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
