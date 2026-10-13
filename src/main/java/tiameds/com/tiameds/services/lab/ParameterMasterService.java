package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.lab.ParameterMasterRequest;
import tiameds.com.tiameds.entity.ParameterMasterEntity;
import tiameds.com.tiameds.repository.ParameterMasterRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ParameterMasterService {

    private final ParameterMasterRepository parameterMasterRepository;

    public ParameterMasterService(ParameterMasterRepository parameterMasterRepository) {
        this.parameterMasterRepository = parameterMasterRepository;
    }

    public List<ParameterMasterEntity> getAllActiveParameters() {
        return parameterMasterRepository.findAllByIsActiveTrue();
    }

    public List<ParameterMasterEntity> getDeletedParameters() {
        return parameterMasterRepository.findAllByIsActiveFalse();
    }

    public ParameterMasterEntity createParameter(ParameterMasterRequest request) {
        if (request.getParameterName() == null || request.getParameterName().isBlank()) {
            throw new IllegalArgumentException("parameterName is required");
        }
        if (parameterMasterRepository.existsByParameterNameIgnoreCase(request.getParameterName().trim())) {
            throw new IllegalArgumentException("Parameter with name '" + request.getParameterName() + "' already exists");
        }
        ParameterMasterEntity entity = new ParameterMasterEntity();
        entity.setParameterName(request.getParameterName().trim());
        entity.setIsActive(true);
        return parameterMasterRepository.save(entity);
    }

    public ParameterMasterEntity updateParameter(Integer parameterId, ParameterMasterRequest request) {
        Optional<ParameterMasterEntity> existing = parameterMasterRepository.findById(parameterId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Parameter with id " + parameterId + " not found");
        }
        ParameterMasterEntity entity = existing.get();
        if (request.getParameterName() != null && !request.getParameterName().isBlank()) {
            String newName = request.getParameterName().trim();
            if (parameterMasterRepository.existsByParameterNameIgnoreCaseAndParameterIdNot(newName, parameterId)) {
                throw new IllegalArgumentException("Parameter with name '" + newName + "' already exists");
            }
            entity.setParameterName(newName);
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        return parameterMasterRepository.save(entity);
    }

    public void deleteParameter(Integer parameterId) {
        ParameterMasterEntity entity = parameterMasterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Parameter with id " + parameterId + " not found"));
        entity.setIsActive(false);
        parameterMasterRepository.save(entity);
    }

    public ParameterMasterEntity activateParameter(Integer parameterId) {
        ParameterMasterEntity entity = parameterMasterRepository.findById(parameterId)
                .orElseThrow(() -> new IllegalArgumentException("Parameter with id " + parameterId + " not found"));
        entity.setIsActive(true);
        return parameterMasterRepository.save(entity);
    }
}
