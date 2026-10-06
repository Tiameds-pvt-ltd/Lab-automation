package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.DescriptionMaster;
import tiameds.com.tiameds.entity.ParameterMaster;

import java.util.List;

@Repository
public interface DescriptionMasterRepository extends JpaRepository<DescriptionMaster, Integer> {

    List<DescriptionMaster> findAllByParameter(ParameterMaster parameter);
}
