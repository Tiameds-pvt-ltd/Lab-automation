package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.TestsMaster;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestsMasterRepository extends JpaRepository<TestsMaster, Integer>, JpaSpecificationExecutor<TestsMaster> {

    boolean existsByTestCode(String testCode);

    boolean existsByName(String name);

    Optional<TestsMaster> findByTestCode(String testCode);

    List<TestsMaster> findAllByIsActive(int isActive);

    Optional<TestsMaster> findByTestIdAndIsActive(Integer testId, int isActive);

    @Query("SELECT DISTINCT t.category FROM TestsMaster t WHERE t.isActive = 1 ORDER BY t.category")
    List<String> findDistinctActiveCategories();
}
