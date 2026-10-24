package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.RadiologyReportSignatory;

import java.util.Collection;
import java.util.List;

public interface RadiologyReportSignatoryRepository extends JpaRepository<RadiologyReportSignatory, Long> {

    List<RadiologyReportSignatory> findByReportIdOrderBySortOrderAsc(Long reportId);

    List<RadiologyReportSignatory> findByReportIdInOrderBySortOrderAsc(Collection<Long> reportIds);

    // Bulk delete runs immediately, so replacing the signatories (delete + insert of the
    // same doctor) cannot trip the unique (report_id, doctor_id) constraint through
    // Hibernate's insert-before-delete flush ordering.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RadiologyReportSignatory s WHERE s.reportId = :reportId")
    void deleteAllByReportId(@Param("reportId") Long reportId);

    // Is this doctor attached to this lab? (lab_doctors is the existing join table.)
    @Query(value = "SELECT COUNT(*) > 0 FROM lab_doctors WHERE lab_id = :labId AND doctor_id = :doctorId", nativeQuery = true)
    boolean isDoctorInLab(@Param("labId") Long labId, @Param("doctorId") Long doctorId);
}
