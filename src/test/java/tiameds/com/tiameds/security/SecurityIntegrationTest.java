package tiameds.com.tiameds.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import tiameds.com.tiameds.config.RateLimitConfig;
import tiameds.com.tiameds.config.IpWhitelistConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "rate.limit.login.attempts=3",
    "rate.limit.login.window=1",
    "rate.limit.ip.attempts=5",
    "rate.limit.ip.window=1",
    "security.ip.whitelist.enabled=false"
})
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitConfig.RateLimitService rateLimitService;

    @Autowired
    private IpWhitelistConfig ipWhitelistConfig;

    @Test
    public void testRateLimiting() throws Exception {
        // Test that rate limiting works by making multiple requests
        String loginRequest = """
            {
                "username": "testuser",
                "password": "wrongpassword"
            }
            """;

        // Make multiple requests to trigger rate limiting
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/public/login")
                    .contentType("application/json")
                    .content(loginRequest)
                    .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().is4xxClientError());
        }

        // The 6th request should be rate limited
        mockMvc.perform(post("/api/v1/public/login")
                .contentType("application/json")
                .content(loginRequest)
                .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void testIpWhitelistDisabled() throws Exception {
        // Test that IP whitelist is disabled by default
        assert !ipWhitelistConfig.isIpWhitelistEnabled();
    }

    @Test
    public void testRateLimitService() {
        // Test rate limit service directly
        String testIp = "192.168.1.200";
        String testUser = "testuser2";

        // Should allow first few attempts
        assert rateLimitService.isIpAllowed(testIp);
        assert rateLimitService.isUserAllowed(testUser);

        // Consume all attempts
        for (int i = 0; i < 5; i++) {
            rateLimitService.isIpAllowed(testIp);
        }

        // Should be rate limited now
        assert !rateLimitService.isIpAllowed(testIp);

        // Reset and test again
        rateLimitService.resetIpLimit(testIp);
        assert rateLimitService.isIpAllowed(testIp);
    }
}
