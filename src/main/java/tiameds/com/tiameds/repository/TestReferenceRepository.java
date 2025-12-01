package tiameds.com.tiameds.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.TestReferenceEntity;

import java.util.Optional;

@Repository
public interface TestReferenceRepository extends JpaRepository<TestReferenceEntity, Long> {

    boolean existsByLabs_Id(Long labId);

    boolean existsByTestReferenceCode(String code);

    Optional<TestReferenceEntity> findTopByTestReferenceCodeStartingWithOrderByTestReferenceCodeDesc(String prefix);
    
    Optional<TestReferenceEntity> findByTestReferenceCode(String testReferenceCode);
}
