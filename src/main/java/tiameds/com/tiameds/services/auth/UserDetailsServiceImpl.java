package tiameds.com.tiameds.services.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.UserRepository;


@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(loginIdentifier)
                .orElseGet(() -> userRepository.findByEmail(loginIdentifier).orElse(null)); // Use Optional

        if (user == null) {
            throw new UsernameNotFoundException("Could not find user with username or email: " + loginIdentifier);
        }

        //set the user details

        log.info("User found: {}", user);
        System.out.println("Roles for user: " + user.getRoles());
        return new MyUserDetails(user);
    }


}
