package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.BillingEntity;
import tiameds.com.tiameds.entity.TestDiscountEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface TestDiscountRepository extends JpaRepository<TestDiscountEntity , Long> {

    Collection<Object> findAllByBillingId(Long id);

    List<TestDiscountEntity> findAllByBilling(BillingEntity billing);

    void deleteByBillingId(Long id);

    void deleteByBilling(BillingEntity billing);
}