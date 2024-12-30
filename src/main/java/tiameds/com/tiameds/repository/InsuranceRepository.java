package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.InsuranceEntity;
import tiameds.com.tiameds.entity.Test;

import java.util.Collection;
import java.util.List;


@Repository
public interface InsuranceRepository extends JpaRepository<InsuranceEntity, Long> {

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END " +
            "FROM InsuranceEntity i JOIN i.labs l " +
            "WHERE i.name = :name AND l.id = :labId")
    boolean existsByNameAndLabId(@Param("name") String name, @Param("labId") Long labId);


//    List<InsuranceEntity> findAllById(List<Long> insuranceIds);

}
