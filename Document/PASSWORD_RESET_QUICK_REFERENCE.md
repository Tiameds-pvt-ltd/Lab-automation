# Password Reset Quick Reference

## API Endpoints

### Forgot Password
```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response:** Always returns success (prevents enumeration)
```json
{
  "status": "success",
  "message": "If an account exists with this email, a password reset link has been sent."
}
```

### Reset Password
```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{
  "token": "base64-token-from-email",
  "newPassword": "NewSecureP@ssw0rd123",
  "confirmPassword": "NewSecureP@ssw0rd123"
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Password has been reset successfully"
}
```

## Password Requirements

- ✅ Minimum 8 characters (max 128)
- ✅ At least one uppercase letter (A-Z)
- ✅ At least one lowercase letter (a-z)
- ✅ At least one digit (0-9)
- ✅ At least one special character (!@#$%^&*()_+-=[]{};':"\|,.<>/?)
- ✅ Must be different from current password

## Rate Limits

- **Per Email:** 3 requests per minute
- **Per IP:** 3 requests per minute
- **Implementation:** Database-backed (PostgreSQL)
- **Storage:** `password_reset_rate_limits` table
- **Shared:** Rate limits shared across all application instances
- **Cleanup:** Automatic cleanup every 5 minutes
- **Persistence:** Rate limits persist across application restarts

## Token Details

- **Length:** 32 bytes (base64 encoded)
- **Expiry:** 15 minutes (configurable to 30)
- **Storage:** SHA-256 hashed in database
- **Usage:** Single-use only

## Configuration

```yaml
password:
  reset:
    token:
      expiry:
        minutes: 15  # or 30
    url: https://app.com/reset-password
    rate:
      limit:
        email:
          max: 3
          window:
            minutes: 1
        ip:
          max: 3
          window:
            minutes: 1
# Note: No block duration needed - rate limits automatically reset after window expires
```

## Error Codes

| Status | Error | Description |
|--------|-------|-------------|
| 200 | Success | Generic success (always returned) |
| 400 | Invalid token | Token expired/used/invalid |
| 400 | Password mismatch | Passwords don't match |
| 400 | Weak password | Doesn't meet requirements |
| 400 | Same password | Must be different from current |
| 429 | Rate limited | Too many requests |
| 500 | Server error | Internal error |

## cURL Examples

### Request Reset
```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'
```

### Reset Password
```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_TOKEN_HERE",
    "newPassword": "NewSecureP@ssw0rd123",
    "confirmPassword": "NewSecureP@ssw0rd123"
  }'
```

## Security Features

✅ Token hashing (SHA-256)  
✅ Rate limiting (Database-backed, shared across instances)  
✅ User enumeration prevention  
✅ Session invalidation  
✅ Audit logging  
✅ Password strength validation  
✅ Single-use tokens  
✅ Short expiry (15 min)  

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Email not received | Check spam, verify SMTP config |
| Token expired | Request new reset (15 min expiry) |
| Rate limited | Wait for window to expire (1 minute) |
| Rate limit not working | Check database connection, verify table exists |
| Password validation fails | Check all requirements |
| Table missing | Run migration script: `database_migration_password_reset_rate_limits.sql` |

## Database Setup

**Run migration script first:**
```bash
psql -U postgres -d tiameds -f Document/database_migration_password_reset_rate_limits.sql
```

## Database Queries

### Check Rate Limits
```sql
SELECT * FROM password_reset_rate_limits 
WHERE rate_limit_key LIKE 'email:%' OR rate_limit_key LIKE 'ip:%'
ORDER BY created_at DESC
LIMIT 20;
```

### Check User Tokens
```sql
SELECT prt.*, u.email 
FROM password_reset_tokens prt
JOIN users u ON prt.user_id = u.user_id
WHERE u.email = 'user@example.com'
ORDER BY prt.created_at DESC;
```

### Check Audit Logs
```sql
SELECT * FROM lab_audit_logs 
WHERE module = 'Authentication' 
AND entity_type = 'PasswordReset'
ORDER BY timestamp DESC
LIMIT 10;
```

## Rate Limit Notes

- Rate limits are stored in **PostgreSQL database** (`password_reset_rate_limits` table)
- Shared across all application instances
- Automatic cleanup every 5 minutes (scheduled job)
- Rate limits persist across application restarts
- Check database for rate limit records
- Ensure table and indexes are created (run migration script)

---

**See [PASSWORD_RESET_FLOW.md](./PASSWORD_RESET_FLOW.md) for complete documentation.**

