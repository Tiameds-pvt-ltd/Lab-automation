package tiameds.com.tiameds.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tiameds.com.tiameds.filter.JwtFilter;
import tiameds.com.tiameds.filter.IpWhitelistFilter;
import tiameds.com.tiameds.filter.RateLimitFilter;
import tiameds.com.tiameds.services.auth.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {
    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final IpWhitelistFilter ipWhitelistFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Autowired
    public SpringSecurityConfig(JwtFilter jwtFilter, UserDetailsServiceImpl userDetailsService, 
                               IpWhitelistFilter ipWhitelistFilter, RateLimitFilter rateLimitFilter,
                               CorsConfigurationSource corsConfigurationSource) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.ipWhitelistFilter = ipWhitelistFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        // Clickjacking Protection
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        // XSS Protection - Content Security Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "object-src 'none'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "frame-ancestors 'self'; " +
                                        "base-uri 'self';"))
                        // HSTS - Prevent HTTP Downgrade
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(true))
                )
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Permit CORS preflight requests

                                // ---------------list of all endpoints with roles as DESKROLE and admin---------------------
                                .requestMatchers(
                                        "/lab/*/visits",
//                                        "/admin/lab/*/doctors",
                                        "/admin/lab/*/packages",
                                        "/lab/*/add-patient",
                                        "/lab/*/patients",
//                                        "/admin/lab/*/doctors/{doctorId}",
                                        "/admin/lab/*/packages",
                                        "/lab/*/update-patient-details/{patientId}",
                                        "/lab/admin/insurance/{labId}"
                                ).hasAnyRole("ADMIN", "DESKROLE","SUPERADMIN")

                                .requestMatchers(
                                        "/admin/lab/*/test/{testId}",
                                        "/admin/lab/*/package/{packageId}",
                                        "/lab/sample-list",
                                        "/lab/*/report/{visitId}",
                                        "/admin/lab/*/tests",
                                        "/lab/test-reference/{labId}",
                                        "/lab/test-reference/{labId}/download",
                                        //------------------------
                                        "/admin/lab/*/doctors",
                                        "/admin/lab/*/doctors/{doctorId}"
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
                                .requestMatchers("/lab/admin/me").hasAnyRole("ADMIN", "TECHNICIAN", "DESKROLE","SUPERADMIN")
                                .requestMatchers("/lab/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
//                                .requestMatchers("/lab/**").hasRole("ADMIN")
                                .requestMatchers("/lab-super-admin/**").hasRole("SUPERADMIN")
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/auth/login", "/auth/register", "/auth/send-otp", "/auth/verify-otp", "/auth/refresh", "/auth/forgot-password", "/auth/reset-password", "/auth/validate-reset-token").permitAll()
                                .requestMatchers("/public/login", "/public/register").permitAll()
                                .requestMatchers("/auth/logout").authenticated()
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
                .addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class)  // Add IP whitelist filter first
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)  // Add rate limit filter
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




