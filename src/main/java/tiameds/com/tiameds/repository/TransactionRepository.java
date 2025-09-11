package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.TransactionEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query("SELECT t FROM TransactionEntity t " +
            "JOIN FETCH t.billing b " +
            "JOIN FETCH b.labs l " +
            "JOIN FETCH b.visit v " +
            "JOIN FETCH v.patient p " +
            "WHERE l.id = :labId " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    List<TransactionEntity> findTransactionsByLabAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}