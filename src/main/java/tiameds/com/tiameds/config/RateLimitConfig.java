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

    @Value("${rate.limit.ip.attempts:10}")
    private int maxIpAttempts;

    @Value("${rate.limit.ip.window:10}")
    private int ipWindowMinutes;

    // In-memory storage for rate limiting (can be replaced with Redis for distributed systems)
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    @Bean
    public RateLimitService rateLimitService() {
        return new RateLimitService(ipBuckets, userBuckets, maxLoginAttempts, loginWindowMinutes, maxIpAttempts, ipWindowMinutes);
    }

    public static class RateLimitService {
        private final Map<String, Bucket> ipBuckets;
        private final Map<String, Bucket> userBuckets;
        private final int maxLoginAttempts;
        private final int loginWindowMinutes;
        private final int maxIpAttempts;
        private final int ipWindowMinutes;

        public RateLimitService(Map<String, Bucket> ipBuckets, Map<String, Bucket> userBuckets, 
                              int maxLoginAttempts, int loginWindowMinutes, 
                              int maxIpAttempts, int ipWindowMinutes) {
            this.ipBuckets = ipBuckets;
            this.userBuckets = userBuckets;
            this.maxLoginAttempts = maxLoginAttempts;
            this.loginWindowMinutes = loginWindowMinutes;
            this.maxIpAttempts = maxIpAttempts;
            this.ipWindowMinutes = ipWindowMinutes;
        }

        public boolean isIpAllowed(String ipAddress) {
            Bucket bucket = ipBuckets.computeIfAbsent(ipAddress, this::createIpBucket);
            return bucket.tryConsume(1);
        }

        public boolean isUserAllowed(String username) {
            Bucket bucket = userBuckets.computeIfAbsent(username, this::createUserBucket);
            return bucket.tryConsume(1);
        }

        public void resetIpLimit(String ipAddress) {
            ipBuckets.remove(ipAddress);
        }

        public void resetUserLimit(String username) {
            userBuckets.remove(username);
        }

        private Bucket createIpBucket(String ipAddress) {
            Bandwidth limit = Bandwidth.classic(maxIpAttempts, Refill.intervally(maxIpAttempts, Duration.ofMinutes(ipWindowMinutes)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        private Bucket createUserBucket(String username) {
            Bandwidth limit = Bandwidth.classic(maxLoginAttempts, Refill.intervally(maxLoginAttempts, Duration.ofMinutes(loginWindowMinutes)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }

        public int getRemainingIpAttempts(String ipAddress) {
            Bucket bucket = ipBuckets.get(ipAddress);
            if (bucket == null) {
                return maxIpAttempts;
            }
            return (int) bucket.getAvailableTokens();
        }

        public int getRemainingUserAttempts(String username) {
            Bucket bucket = userBuckets.get(username);
            if (bucket == null) {
                return maxLoginAttempts;
            }
            return (int) bucket.getAvailableTokens();
        }
    }
}
