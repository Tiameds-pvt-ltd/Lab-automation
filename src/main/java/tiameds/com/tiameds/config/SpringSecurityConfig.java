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


//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        return http
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeRequests(req -> req
//
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS pre-flight requests
//                        // Role-based authorization first
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/lab/**").hasRole("ADMIN")
//                        .requestMatchers("/lab-super-admin/**").hasRole("SUPERADMIN")
//                        .requestMatchers("/error").permitAll()
//                        // Allow login and registration paths to be accessed without authentication
//                        .requestMatchers("/login/**", "/register/**").permitAll()
//
//                        // Allow Swagger and public resources to be accessed without authentication
//                        .requestMatchers(
//                                "/v3/api-docs/**",
//                                "/doc/**",
//                                "/swagger-ui/**",
//                                "/swagger-ui.html",
//                                "/public/**"
//                        ).permitAll()
//
//                        // Catch-all for any other request, ensuring that they are authenticated
//                        .anyRequest().authenticated()
//                )
//                .userDetailsService(userDetailsService)
//                .sessionManagement(c -> c
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Stateless (JWT) authentication
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)  // JWT filter for authentication
//                .build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless JWT authentication
                .cors(cors -> cors.configurationSource(new CorsConfig().corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Permit CORS preflight requests
                                .requestMatchers("/admin/**").hasRole("ADMIN")  // Restrict /admin/** to ADMIN role
                                .requestMatchers("/lab/admin/**").hasRole("ADMIN")  // Restrict /lab/admin/** to ADMIN role
                                .requestMatchers("/lab/**").hasRole("ADMIN")    // Restrict /lab/** to ADMIN role
                                .requestMatchers("/lab-super-admin/**").hasRole("SUPERADMIN") // SUPERADMIN-only endpoints
                                .requestMatchers("/error").permitAll()          // Allow error endpoint without authentication
                                .requestMatchers("/login/**", "/register/**").permitAll()  // Public login & registration
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




