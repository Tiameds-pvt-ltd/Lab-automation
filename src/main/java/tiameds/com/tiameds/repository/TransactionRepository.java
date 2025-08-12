package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

}