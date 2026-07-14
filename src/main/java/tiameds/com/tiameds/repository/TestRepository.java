package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findByLabs(Lab lab);

    boolean existsByLabs_Id(Long labId);
    boolean existsByTestCode(String testCode);

    java.util.Optional<Test> findTopByTestCodeStartingWithOrderByTestCodeDesc(String testCodePrefix);

    @Query("SELECT COUNT(t) FROM Test t JOIN t.labs l WHERE l.id = :labId")
    long countByLabIdAllTime(@Param("labId") Long labId);

    @Query("SELECT COUNT(t) FROM Test t JOIN t.labs l WHERE l.id = :labId AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT t) FROM Test t JOIN t.labs l WHERE l.createdBy = :createdBy")
    long countByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(DISTINCT t) FROM Test t JOIN t.labs l WHERE l.createdBy = :createdBy AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT t.category AS category, COUNT(DISTINCT t.test_id) AS testCount " +
            "FROM tests t " +
            "JOIN lab_tests lt ON t.test_id = lt.test_id " +
            "JOIN labs l ON lt.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "GROUP BY t.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getTestsByCategory(@Param("createdById") Long createdById);

    @Query(value = "SELECT t.category AS category, COUNT(DISTINCT t.test_id) AS testCount " +
            "FROM tests t " +
            "JOIN lab_tests lt ON t.test_id = lt.test_id " +
            "JOIN labs l ON lt.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getTestsByCategoryWithDateRange(@Param("createdById") Long createdById, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT t.category AS category, COUNT(DISTINCT t.test_id) AS testCount " +
            "FROM tests t " +
            "JOIN lab_tests lt ON t.test_id = lt.test_id " +
            "WHERE lt.lab_id = :labId " +
            "GROUP BY t.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getTestsByCategoryByLabId(@Param("labId") Long labId);

    @Query(value = "SELECT t.category AS category, COUNT(DISTINCT t.test_id) AS testCount " +
            "FROM tests t " +
            "JOIN lab_tests lt ON t.test_id = lt.test_id " +
            "WHERE lt.lab_id = :labId " +
            "AND t.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getTestsByCategoryByLabIdWithDateRange(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    interface TestsByCategoryProjection {
        String getCategory();
        Long getTestCount();
    }
}