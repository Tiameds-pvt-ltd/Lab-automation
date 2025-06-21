package tiameds.com.tiameds.controller.auth;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.auth.AuthResponse;
import tiameds.com.tiameds.dto.auth.LoginRequest;
import tiameds.com.tiameds.dto.auth.LoginResponse;
import tiameds.com.tiameds.dto.auth.RegisterRequest;
import tiameds.com.tiameds.dto.lab.ModuleDTO;
import tiameds.com.tiameds.entity.ModuleEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.ModuleRepository;
import tiameds.com.tiameds.services.auth.UserDetailsServiceImpl;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.JwtUtil;
import tiameds.com.tiameds.entity.Role;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/public")
@Tag(name = "User Controller", description = "Operations pertaining to user management ")
public class  UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtils; 
    private final ModuleRepository moduleRepository;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder, JwtUtil jwtUtils, ModuleRepository moduleRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.moduleRepository = moduleRepository;
    }

    @GetMapping("/health-check")
    public String healthCheck() {
        return "Service is up  and running";
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = null;
        try {
            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Return error response
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(HttpStatus.BAD_REQUEST , "Incorrect username or password", null, null));
        }

        // Load user details
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

        // Generate JWT token
        token = jwtUtils.generateToken(userDetails.getUsername());

        // Fetch user details
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(HttpStatus.BAD_REQUEST, "User not found", null, null));
        }
        User user = userOptional.get();

        // Convert roles to list of strings
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // Fetch modules
        Set<ModuleEntity> modules = user.getModules();
        List<ModuleDTO> moduleDTOList = new ArrayList<>();
        for (ModuleEntity module : modules) {
            ModuleDTO moduleDTO = new ModuleDTO();
            moduleDTO.setId(module.getId());
            moduleDTO.setName(module.getName());
            moduleDTOList.add(moduleDTO);
        }

        // Create the LoginResponse object
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUsername(user.getUsername());
        loginResponse.setEmail(user.getEmail());
        loginResponse.setFirstName(user.getFirstName());
        loginResponse.setLastName(user.getLastName());
        loginResponse.setRoles(roles);
        loginResponse.setPhone(user.getPhone());
        loginResponse.setAddress(user.getAddress());
        loginResponse.setCity(user.getCity());
        loginResponse.setState(user.getState());
        loginResponse.setZip(user.getZip());
        loginResponse.setCountry(user.getCountry());
        loginResponse.setVerified(user.isVerified());
        loginResponse.setEnabled(user.isEnabled());
//        loginResponse.setModules(moduleDTOList);

        // Create and return the AuthResponse
        return ResponseEntity.ok(new AuthResponse(
                HttpStatus.OK,
                "Login successful", token, loginResponse));
    }


//    @PostMapping("/register")
//    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
//
//        if (userService.existsByUsername(registerRequest.getUsername())) {
//            return ApiResponseHelper.successResponseWithDataAndMessage("Username is already taken", HttpStatus.BAD_REQUEST,null);
//        }
//        if (userService.existsByEmail(registerRequest.getEmail())) {
//            return ApiResponseHelper.successResponseWithDataAndMessage("Email is already taken", HttpStatus.BAD_REQUEST,null);
//        }
//
//        List<Long> moduleIds = registerRequest.getModules();
//        Set<ModuleEntity> modules = new HashSet<>();
//
//        // Iterate over the moduleIds and fetch corresponding ModuleEntity objects
//        for (Long moduleId : moduleIds) {
//            Optional<ModuleEntity> moduleOptional = moduleRepository.findById(moduleId);
//            if (!moduleOptional.isPresent()) {
//                return ApiResponseHelper.successResponseWithDataAndMessage("Module not found", HttpStatus.BAD_REQUEST,null);
//            }
//            modules.add(moduleOptional.get());
//        }
//        // Create a new User
//        User user = new User();
//        user.setUsername(registerRequest.getUsername());
//        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));  // Encrypt password
//        user.setEmail(registerRequest.getEmail());
//        user.setFirstName(registerRequest.getFirstName());
//        user.setLastName(registerRequest.getLastName());
//        user.setPhone(registerRequest.getPhone());
//        user.setAddress(registerRequest.getAddress());
//        user.setCity(registerRequest.getCity());
//        user.setState(registerRequest.getState());
//        user.setZip(registerRequest.getZip());
//        user.setCountry(registerRequest.getCountry());
//        user.setVerified(registerRequest.isVerified());
//        user.setEnabled(true);
//        user.setModules(modules);
//        userService.saveUser(user);
//
//        return ApiResponseHelper.successResponseWithDataAndMessage("User registered successfully", HttpStatus.CREATED,null);
//    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {

        if (userService.existsByUsername(registerRequest.getUsername())) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Username is already taken", HttpStatus.BAD_REQUEST,null);
        }
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Email is already taken", HttpStatus.BAD_REQUEST,null);
        }

//        List<Long> moduleIds = registerRequest.getModules();
//        Set<ModuleEntity> modules = new HashSet<>();
//
//        // Iterate over the moduleIds and fetch corresponding ModuleEntity objects
//        for (Long moduleId : moduleIds) {
//            Optional<ModuleEntity> moduleOptional = moduleRepository.findById(moduleId);
//            if (!moduleOptional.isPresent()) {
//                return ApiResponseHelper.successResponseWithDataAndMessage("Module not found", HttpStatus.BAD_REQUEST,null);
//            }
//            modules.add(moduleOptional.get());
//        }
        // Create a new User
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));  // Encrypt password
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
        user.setEnabled(true);
//        user.setModules(modules);
        userService.saveUser(user);
        return ApiResponseHelper.successResponseWithDataAndMessage("User registered successfully", HttpStatus.CREATED,null);
    }

}

