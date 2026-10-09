package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.ParameterMasterEntity;

import java.util.List;

public interface ParameterMasterRepository extends JpaRepository<ParameterMasterEntity, Integer> {

    List<ParameterMasterEntity> findAllByIsActiveTrue();
}
