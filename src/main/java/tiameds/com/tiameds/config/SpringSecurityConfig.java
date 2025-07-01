package tiameds.com.tiameds.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tiameds.com.tiameds.filter.JwtFilter;
import tiameds.com.tiameds.services.auth.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public SpringSecurityConfig(JwtFilter jwtFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());        // Set the password encoder
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless JWT authentication
                .cors(cors -> cors.configurationSource(new CorsConfig().corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Permit CORS preflight requests


                                // ---------------list of all endpoints with roles as DESKROLE and admin---------------------
                                .requestMatchers(
                                        "/lab/*/visits",
                                        "/admin/lab/*/doctors",
                                        "/admin/lab/*/packages",
                                        "/lab/*/add-patient",
                                        "/lab/*/patients",
                                        "/admin/lab/*/doctors/{doctorId}",
                                        "/admin/lab/*/doctors",
                                        "/admin/lab/*/packages",
                                        "/admin/lab/*/doctors",
                                        "/lab/*/update-patient-details/{patientId}",
                                        "/admin/lab/*/doctors/{doctorId}",
                                        "/lab/admin/insurance/{labId}"
                                ).hasAnyRole("ADMIN", "DESKROLE","SUPER ADMIN")

                                .requestMatchers(
                                        "/admin/lab/*/test/{testId}",
                                        "/admin/lab/*/package/{packageId}",
                                        "/lab/sample-list",
                                        "/lab/*/report/{visitId}",
                                        "/admin/lab/*/tests",
                                        "/lab/test-reference/{labId}",
                                        "/lab/test-reference/{labId}/download"
                                ).hasAnyRole("ADMIN", "DESKROLE", "TECHNICIAN","SUPERADMIN")

                                // ---------------list of all endpoints with roles as TECHNICIAN and admin---------------------
                                .requestMatchers(
                                        "/lab/*/visitsdatewise",
                                        "/lab/add-samples",
                                        "/lab/update-samples",
                                        "/lab/delete-samples",
                                        "/lab/*/get-visit-samples",
                                        "lab/*/report",
                                        "lab/test-reference/{labId}/test/{testName}",
                                        "lab/*/report/{visitId}",
                                        "/lab/*/complete-visit/{visitId}",
                                        "lab/test-reference/{labId}/add",
                                        "lab/test-reference/{labId}/{testReferenceId}"
                                ).hasAnyRole("ADMIN", "TECHNICIAN","SUPERADMIN")

                                .requestMatchers(
                                        "/lab/*/datewise-lab-visits"
                                ).hasAnyRole("ADMIN", "TECHNICIAN","SUPERADMIN", "DESKROLE")

                        //---------- admin and super admin endpoints -------------------
                                .requestMatchers(
                                        "admin/lab/*/test/{testId}",
                                        "admin/lab/*/test",
                                        "admin/lab/*/package/{packageId}",
                                        "admin/lab/*/package",
                                        "admin/lab/*/test-reference/{testReferenceId}",
                                        "admin/lab/*/test-reference",
                                        "admin/lab/*/test-reference/{testReferenceId}/update",
                                        "admin/lab/*/test-reference/{testReferenceId}/remove",
                                        "admin/lab/*/test-reference/add",
                                        "admin/lab/*/add",
                                        "admin/lab/*/update/{testId}",
                                        "admin/lab/*/remove/{testId}",
                                        "/lab/static/{labId}",
                                        "lab/admin/get-labs",
//                                        /admin/lab/18/download
                                        "/admin/lab/{labId}/download"
                                ).hasAnyRole("SUPERADMIN", "ADMIN")


                              //-------------userManagement endpoints for admin and super admin-------------------
                                .requestMatchers(
                                        "/user-management/get-members/**",
                                        "/user-management/create-user/**",
                                        "/user-management/update-user/**",
                                        "/user-management/reset-password/**",
                                        "/user-management/delete-user/**",
                                        "/user-management/get-user/**"
                                ).hasAnyRole("ADMIN", "SUPERADMIN")


                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/lab/admin/get-user-labs").hasAnyRole("ADMIN", "TECHNICIAN", "DESKROLE","SUPERADMIN")
                                .requestMatchers("/lab/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
//                                .requestMatchers("/lab/**").hasRole("ADMIN")
                                .requestMatchers("/lab-super-admin/**").hasRole("SUPERADMIN")
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/login/**", "/register/**").permitAll()
                                .requestMatchers("/api/v1/public/health-check").permitAll()
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/doc/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/public/**"
                                ).permitAll()  // Allow Swagger and public resources
                                .anyRequest().authenticated()  // All other requests must be authenticated
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Stateless (JWT) sessions
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)  // Add JWT filter
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}




