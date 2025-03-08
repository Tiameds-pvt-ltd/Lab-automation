package tiameds.com.tiameds.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.TestReferenceEntity;

import java.util.List;

@Repository
public interface TestReferenceRepository extends JpaRepository<TestReferenceEntity, Long> {

}
