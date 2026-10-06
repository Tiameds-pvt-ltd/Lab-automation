package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.ParameterMaster;
import tiameds.com.tiameds.entity.TestsMaster;

import java.util.List;

@Repository
public interface ParameterMasterRepository extends JpaRepository<ParameterMaster, Integer> {

    List<ParameterMaster> findAllByTest(TestsMaster test);
}
