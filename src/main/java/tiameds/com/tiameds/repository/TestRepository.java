package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TestRepository extends JpaRepository<Test, Long> {


    List<Test> findByLabs(Lab lab);

//    long countByLabId(Long labId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT COUNT(t) FROM Test t JOIN t.labs l WHERE l.id = :labId AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


}