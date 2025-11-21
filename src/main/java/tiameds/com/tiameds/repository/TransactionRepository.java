package tiameds.com.tiameds.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.TransactionEntity;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query(value = "SELECT DISTINCT t FROM TransactionEntity t " +
            "JOIN t.billing b " +
            "JOIN b.labs l " +
            "JOIN b.visit v " +
            "JOIN v.patient p " +
            "WHERE l.id = :labId " +
            "AND t.createdAt BETWEEN :startDate AND :endDate",
            countQuery = "SELECT COUNT(DISTINCT t) FROM TransactionEntity t " +
                    "JOIN t.billing b " +
                    "JOIN b.labs l " +
                    "JOIN b.visit v " +
                    "WHERE l.id = :labId " +
                    "AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<TransactionEntity> findTransactionsByLabAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}