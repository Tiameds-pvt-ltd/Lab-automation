package tiameds.com.tiameds.services.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.dto.auth.MemberDetailsUpdate;
import tiameds.com.tiameds.dto.auth.MemberRegisterDto;
import tiameds.com.tiameds.dto.lab.UserInLabDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.RoleRepository;
import tiameds.com.tiameds.repository.UserRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberUserServices {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public MemberUserServices(PasswordEncoder passwordEncoder, UserRepository userRepository, RoleRepository roleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public static List<UserInLabDTO> getMembersInLab(Lab lab) {
        return lab.getMembers().stream()
                .map(user -> new UserInLabDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.isEnabled(),
                        user.getPhone(),
                        user.getCity(),
                        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()
                        ))).collect(Collectors.toList());
    }

    public void updateUserInLab(MemberDetailsUpdate registerRequest, User userToUpdate, Lab lab, User currentUser) {
        userToUpdate.setUsername(registerRequest.getUsername());
        userToUpdate.setEmail(registerRequest.getEmail());
        userToUpdate.setFirstName(registerRequest.getFirstName());
        userToUpdate.setLastName(registerRequest.getLastName());
        userToUpdate.setPhone(registerRequest.getPhone());
        userToUpdate.setAddress(registerRequest.getAddress());
        userToUpdate.setCity(registerRequest.getCity());
        userToUpdate.setState(registerRequest.getState());
        userToUpdate.setZip(registerRequest.getZip());
        userToUpdate.setCountry(registerRequest.getCountry());
        userToUpdate.setVerified(registerRequest.isVerified());
        userToUpdate.setEnabled(registerRequest.getEnabled());
        userToUpdate.setCreatedBy(currentUser);
        Set<Role> roles = registerRequest.getRoles()
                .stream()
                .map(roleName -> roleRepository.findByName(String.valueOf(roleName))
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName(String.valueOf(roleName));
                            return roleRepository.save(newRole);
                        }))
                .collect(Collectors.toSet());
        userToUpdate.setRoles(roles);
        try {
            userRepository.save(userToUpdate);
            lab.getMembers().add(userToUpdate);
        } catch (Exception e) {
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }


    public void createUserAndAddToLab(MemberRegisterDto registerRequest, Lab lab, User currentUser) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        user.setCity(registerRequest.getCity());
        user.setState(registerRequest.getState());
        user.setZip(registerRequest.getZip());
        user.setCountry(registerRequest.getCountry());
        user.setVerified(registerRequest.isVerified());
        user.setEnabled(registerRequest.getEnabled());
        user.setCreatedBy(currentUser);
        user.setRoles(registerRequest.getRoles()
                .stream()
                .map(roleName -> roleRepository.findByName(String.valueOf(roleName))
                        //if no role found, create a new one
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName(String.valueOf(roleName));

                            return roleRepository.save(newRole);
                        }))
                .collect(Collectors.toSet()));
        user.setLabs(Set.of(lab));
        try {
            userRepository.save(user);
            lab.getMembers().add(user);
        } catch (Exception e) {
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
    }
}