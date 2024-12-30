package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.VisitEntity;

import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<VisitEntity, Long> {



    List<VisitEntity> findAllByPatient_Labs(Lab lab);

    List<VisitEntity> findAllByPatient(PatientEntity patientEntity);

    @Query("SELECT v FROM VisitEntity v WHERE v.patient.patientId = :patientId")
    List<VisitEntity> findByPatientId(@Param("patientId") Long patientId);

}


//@Repository
//public interface VisitRepository extends JpaRepository<VisitEntity, Long> {
//
//    // Fetch visits by lab ID with full relationships
//    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient p JOIN FETCH p.labs l WHERE l.id = :labId")
//    List<VisitEntity> findAllByLabId(@Param("labId") Long labId);
//
//    // Fetch visits by Lab entity with full relationships
//    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient p WHERE :lab MEMBER OF p.labs")
//    List<VisitEntity> findAllByPatient_Labs(@Param("lab") Lab lab);
//
//    // Fetch visits by Patient entity
//    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient p WHERE p = :patientEntity")
//    List<VisitEntity> findAllByPatient(@Param("patientEntity") PatientEntity patientEntity);
//
//    // Fetch visits by Patient ID
//    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient p WHERE p.patientId = :patientId")
//    List<VisitEntity> findByPatientId(@Param("patientId") Long patientId);
//}
//
