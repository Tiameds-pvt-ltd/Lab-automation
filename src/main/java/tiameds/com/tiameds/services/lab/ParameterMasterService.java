package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.ParameterMasterEntity;
import tiameds.com.tiameds.repository.ParameterMasterRepository;

import java.util.List;

@Service
public class ParameterMasterService {

    private final ParameterMasterRepository parameterMasterRepository;

    public ParameterMasterService(ParameterMasterRepository parameterMasterRepository) {
        this.parameterMasterRepository = parameterMasterRepository;
    }

    public List<ParameterMasterEntity> getAllActiveParameters() {
        return parameterMasterRepository.findAllByIsActiveTrue();
    }
}
