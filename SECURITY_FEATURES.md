# Security Features Implementation

This document describes the security features implemented for the Lab Automation system.

## Rate Limiting

### Overview
The system implements rate limiting using Bucket4j to prevent brute-force login attempts. Rate limiting is applied at both IP and user levels.

### Configuration
Rate limiting can be configured in `application.yml`:

```yaml
rate:
  limit:
    login:
      attempts: 5  # Max login attempts per user
      window: 10   # Time window in minutes
    ip:
      attempts: 10  # Max login attempts per IP
      window: 10    # Time window in minutes
```

### Features
- **Per-IP Rate Limiting**: Limits login attempts from a single IP address
- **Per-User Rate Limiting**: Limits login attempts for a specific username
- **Configurable Limits**: Both limits can be configured via application properties
- **Automatic Reset**: Rate limits reset after the configured time window
- **Manual Reset**: Admins can manually reset rate limits via API endpoints

### API Endpoints
- `GET /api/v1/public/rate-limit-status?ipAddress={ip}` - Check remaining attempts for an IP
- `GET /api/v1/public/rate-limit-status?username={username}` - Check remaining attempts for a user
- `POST /api/v1/public/reset-rate-limit?ipAddress={ip}` - Reset rate limit for an IP
- `POST /api/v1/public/reset-rate-limit?username={username}` - Reset rate limit for a user

## IP Whitelisting

### Overview
The system includes an IP whitelist filter that can restrict access to login endpoints based on client IP addresses.

### Configuration
IP whitelisting can be configured in `application.yml`:

```yaml
security:
  ip:
    whitelist:
      enabled: false  # Set to true to enable IP whitelisting
      ips: "192.168.1.1,10.0.0.1"  # Comma-separated list of allowed IPs
```

### Features
- **Configurable**: Can be enabled/disabled via configuration
- **Dynamic Management**: IPs can be added/removed at runtime via API
- **X-Forwarded-For Support**: Properly handles requests behind proxies
- **Login-Only**: Only applies to authentication endpoints

### API Endpoints
- `GET /api/v1/admin/security/ip-whitelist/status` - Get whitelist status
- `POST /api/v1/admin/security/ip-whitelist/add?ipAddress={ip}` - Add IP to whitelist
- `DELETE /api/v1/admin/security/ip-whitelist/remove?ipAddress={ip}` - Remove IP from whitelist

## Implementation Details

### Filters
1. **IpWhitelistFilter**: Checks client IP against whitelist
2. **RateLimitFilter**: Applies rate limiting to login attempts
3. **JwtFilter**: Handles JWT authentication (existing)

### Filter Order
Filters are applied in the following order:
1. IP Whitelist Filter
2. Rate Limit Filter
3. JWT Filter

### Dependencies
- `bucket4j-core`: Core rate limiting functionality

## Security Considerations

### Rate Limiting
- Uses in-memory storage (suitable for single-instance deployments)
- Rate limits are per-application-instance
- Simple and lightweight implementation

### IP Whitelisting
- Only applies to login endpoints (`/login`, `/auth`)
- Supports both IPv4 and IPv6 addresses
- Handles proxy headers (`X-Forwarded-For`, `X-Real-IP`)
- Can be bypassed if disabled in configuration

### Error Responses
- Rate limit exceeded: HTTP 429 (Too Many Requests)
- IP not whitelisted: HTTP 403 (Forbidden)
- Standard error format with JSON response body

## Testing

The implementation includes integration tests that verify:
- Rate limiting functionality
- IP whitelist behavior
- Configuration loading
- API endpoint responses

Run tests with:
```bash
mvn test -Dtest=SecurityIntegrationTest
```

## Monitoring

### Logs
- Rate limit violations are logged at WARN level
- IP whitelist violations are logged at WARN level
- All security events include relevant context (IP, username, etc.)

### Metrics
- Rate limit status can be checked via API endpoints
- Remaining attempts are tracked and exposed via API
- Admin endpoints provide management capabilities

## Production Deployment

### Recommendations
1. Enable IP whitelisting in production environments
2. Configure appropriate rate limits based on expected traffic
3. Monitor rate limit violations and adjust limits as needed
4. Regularly review and update IP whitelist
5. For multi-instance deployments, consider load balancer-level rate limiting

### Configuration Example
```yaml
rate:
  limit:
    login:
      attempts: 3
      window: 15
    ip:
      attempts: 20
      window: 15

security:
  ip:
    whitelist:
      enabled: true
      ips: "192.168.1.0/24,10.0.0.0/8"
```
