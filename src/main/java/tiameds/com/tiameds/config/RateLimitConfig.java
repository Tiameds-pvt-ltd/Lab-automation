package tiameds.com.tiameds.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; 

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    @Value("${rate.limit.login.attempts:5}")
    private int maxLoginAttempts;

    @Value("${rate.limit.login.window:10}")
    private int loginWindowMinutes;

    // In-memory storage for user-based rate limiting (can be replaced with Redis for distributed systems)
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    @Bean
    public RateLimitService rateLimitService() {
        return new RateLimitService(userBuckets, maxLoginAttempts, loginWindowMinutes);
    }

    public static class RateLimitService {
        private final Map<String, Bucket> userBuckets;
        private final int maxLoginAttempts;
        private final int loginWindowMinutes;

        public RateLimitService(Map<String, Bucket> userBuckets, 
                              int maxLoginAttempts, int loginWindowMinutes) {
            this.userBuckets = userBuckets;
            this.maxLoginAttempts = maxLoginAttempts;
            this.loginWindowMinutes = loginWindowMinutes;
        }

        /**
         * Checks if the user is allowed to make a login attempt.
         * Blocks the user for the configured time window (default 10 minutes) if limit is exceeded.
         * 
         * @param username The username or email to check
         * @return true if user is allowed, false if rate limit exceeded
         */
        public boolean isUserAllowed(String username) {
            if (username == null || username.trim().isEmpty()) {
                return true; // Allow if username is not provided (will fail authentication anyway)
            }
            String normalizedUsername = username.toLowerCase().trim();
            Bucket bucket = userBuckets.computeIfAbsent(normalizedUsername, this::createUserBucket);
            return bucket.tryConsume(1);
        }

        /**
         * Resets the rate limit for a specific user.
         * 
         * @param username The username to reset
         */
        public void resetUserLimit(String username) {
            if (username != null) {
                userBuckets.remove(username.toLowerCase().trim());
            }
        }

        /**
         * Creates a rate limit bucket for a user.
         * The bucket allows maxLoginAttempts attempts within loginWindowMinutes window.
         */
        private Bucket createUserBucket(String username) {
            Bandwidth limit = Bandwidth.classic(
                maxLoginAttempts, 
                Refill.intervally(maxLoginAttempts, Duration.ofMinutes(loginWindowMinutes))
            );
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        /**
         * Gets the remaining login attempts for a user.
         * 
         * @param username The username to check
         * @return Number of remaining attempts
         */
        public int getRemainingUserAttempts(String username) {
            if (username == null || username.trim().isEmpty()) {
                return maxLoginAttempts;
            }
            String normalizedUsername = username.toLowerCase().trim();
            Bucket bucket = userBuckets.get(normalizedUsername);
            if (bucket == null) {
                return maxLoginAttempts;
            }
            return (int) bucket.getAvailableTokens();
        }

        /**
         * Gets the configured time window in minutes.
         * 
         * @return Time window in minutes
         */
        public int getWindowMinutes() {
            return loginWindowMinutes;
        }
    }
}
