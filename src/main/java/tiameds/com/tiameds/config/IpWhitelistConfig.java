package tiameds.com.tiameds.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Component
public class IpWhitelistConfig {

    @Value("${security.ip.whitelist.enabled:false}")
    private boolean ipWhitelistEnabled;

    @Value("${security.ip.whitelist.ips:}")
    private String whitelistedIps;

    private Set<String> whitelistSet;

    public boolean isIpWhitelistEnabled() {
        return ipWhitelistEnabled;
    }

    public Set<String> getWhitelistedIps() {
        if (whitelistSet == null) {
            whitelistSet = new HashSet<>();
            if (whitelistedIps != null && !whitelistedIps.trim().isEmpty()) {
                List<String> ips = Arrays.asList(whitelistedIps.split(","));
                for (String ip : ips) {
                    whitelistSet.add(ip.trim());
                }
            }
        }
        return whitelistSet;
    }

    public boolean isIpAllowed(String ipAddress) {
        if (!ipWhitelistEnabled) {
            return true; // If whitelist is disabled, allow all IPs
        }
        
        Set<String> whitelist = getWhitelistedIps();
        if (whitelist.isEmpty()) {
            return true; // If no IPs are whitelisted, allow all
        }
        
        return whitelist.contains(ipAddress);
    }

    public void addIpToWhitelist(String ipAddress) {
        getWhitelistedIps().add(ipAddress);
    }

    public void removeIpFromWhitelist(String ipAddress) {
        getWhitelistedIps().remove(ipAddress);
    }
}
