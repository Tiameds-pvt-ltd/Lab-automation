package tiameds.com.tiameds.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://*.vercel.app",
                "https://tiameds-lab-app.vercel.app/",
                "https://tiameds-lab-app-git-main-abhishek-kumars-projects-7cc8d4a1.vercel.app/",
                "https://tiameds-lab-541wzlkz6-abhishek-kumars-projects-7cc8d4a1.vercel.app/"
                ));  // Allow frontend origin
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));  // Allow these HTTP methods
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));  // Headers allowed in requests
        configuration.setAllowCredentials(true);  // Allow credentials like cookies
        configuration.setMaxAge(3600L);  // Cache preflight responses for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}