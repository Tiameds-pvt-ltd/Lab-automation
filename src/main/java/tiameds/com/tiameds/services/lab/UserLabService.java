package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;

@Slf4j
@Service
@Transactional
public class UserLabService {

    private final UserRepository userRepository;
    private final LabRepository labRepository;

    public UserLabService(UserRepository userRepository, LabRepository labRepository) {
        this.userRepository = userRepository;
        this.labRepository = labRepository;
    }

    public boolean existsLabByName(String name) {
        return labRepository.existsByName(name);
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional
    public Lab getLabWithMembers(Long labId) {
        return labRepository.findById(labId).orElse(null);
    }
}
