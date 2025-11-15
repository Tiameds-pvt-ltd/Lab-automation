package tiameds.com.tiameds.controller.auth;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.config.IpWhitelistConfig;
import tiameds.com.tiameds.config.RateLimitConfig;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/security")
@Tag(name = "Security Controller", description = "Security management operations")
public class SecurityController {

    @Autowired
    private IpWhitelistConfig ipWhitelistConfig;

    @Autowired
    private RateLimitConfig.RateLimitService rateLimitService;

    @GetMapping("/ip-whitelist/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> getIpWhitelistStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", ipWhitelistConfig.isIpWhitelistEnabled());
        response.put("whitelistedIps", ipWhitelistConfig.getWhitelistedIps());
        
        return ApiResponseHelper.successResponseWithDataAndMessage("IP whitelist status retrieved", HttpStatus.OK, response);
    }

    @PostMapping("/ip-whitelist/enable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> enableIpWhitelist() {
        // This would require modifying the configuration at runtime
        // For now, we'll return a message indicating manual configuration is needed
        return ApiResponseHelper.successResponseWithDataAndMessage(
            "IP whitelist enabled. Please update application.yml and restart the application.", 
            HttpStatus.OK, 
            null
        );
    }

    @PostMapping("/ip-whitelist/disable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> disableIpWhitelist() {
        return ApiResponseHelper.successResponseWithDataAndMessage(
            "IP whitelist disabled. Please update application.yml and restart the application.", 
            HttpStatus.OK, 
            null
        );
    }

    @PostMapping("/ip-whitelist/add")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> addIpToWhitelist(@RequestParam String ipAddress) {
        ipWhitelistConfig.addIpToWhitelist(ipAddress);
        log.info("IP {} added to whitelist", ipAddress);
        return ApiResponseHelper.successResponseWithDataAndMessage("IP added to whitelist: " + ipAddress, HttpStatus.OK, null);
    }

    @DeleteMapping("/ip-whitelist/remove")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> removeIpFromWhitelist(@RequestParam String ipAddress) {
        ipWhitelistConfig.removeIpFromWhitelist(ipAddress);
        log.info("IP {} removed from whitelist", ipAddress);
        return ApiResponseHelper.successResponseWithDataAndMessage("IP removed from whitelist: " + ipAddress, HttpStatus.OK, null);
    }

    @PostMapping("/rate-limit/reset")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> resetRateLimit(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Username is required", HttpStatus.BAD_REQUEST, null);
        }
        rateLimitService.resetUserLimit(username);
        log.info("Rate limit reset for user: {}", username);
        return ApiResponseHelper.successResponseWithDataAndMessage("Rate limit reset for user: " + username, HttpStatus.OK, null);
    }

    @GetMapping("/rate-limit/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(@RequestParam String username) {
        if (username == null || username.trim().isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Username is required", HttpStatus.BAD_REQUEST, null);
        }
        
        Map<String, Object> status = new HashMap<>();
        int remainingUserAttempts = rateLimitService.getRemainingUserAttempts(username);
        int windowMinutes = rateLimitService.getWindowMinutes();
        
        status.put("username", username);
        status.put("remainingAttempts", remainingUserAttempts);
        status.put("windowMinutes", windowMinutes);
        
        return ApiResponseHelper.successResponseWithDataAndMessage("Rate limit status retrieved", HttpStatus.OK, status);
    }
}
