# OTP Authentication - Quick Reference Guide

## Quick Start

### 1. Environment Variables Setup

**Development:**
```bash
export AWS_SES_REGION=us-east-1
export AWS_SES_ACCESS_KEY=your-access-key
export AWS_SES_SECRET_KEY=your-secret-key
export AWS_SES_SENDER_EMAIL=noreply@yourdomain.com
```

**Test:**
```bash
export AWS_SES_REGION=us-east-1
export AWS_SES_ACCESS_KEY=your-access-key
export AWS_SES_SECRET_KEY=your-secret-key
export AWS_SES_SENDER_EMAIL=noreply@yourdomain.com
```

**Production:**
```bash
export AWS_SES_REGION=us-east-1
export AWS_SES_ACCESS_KEY=your-access-key
export AWS_SES_SECRET_KEY=your-secret-key
export AWS_SES_SENDER_EMAIL=noreply@yourdomain.com
```

### 2. Database Setup

```sql
CREATE TABLE otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_otp_email ON otps(email);
CREATE INDEX idx_otp_expires_at ON otps(expires_at);
```

---

## API Endpoints

### Send OTP
```http
POST /api/v1/auth/send-otp
Content-Type: application/json

{
  "email": "user@example.com"
}
```

### Verify OTP
```http
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

---

## cURL Examples

### Send OTP
```bash
curl -X POST http://localhost:8080/api/v1/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'
```

### Verify OTP
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "otp": "123456"}' \
  -v
```

---

## JavaScript Examples

### Send OTP
```javascript
const response = await fetch('/api/v1/auth/send-otp', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'user@example.com' })
});
const data = await response.json();
```

### Verify OTP
```javascript
const response = await fetch('/api/v1/auth/verify-otp', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // Important for cookies
  body: JSON.stringify({ 
    email: 'user@example.com', 
    otp: '123456' 
  })
});
const data = await response.json();
```

---

## Configuration Constants

| Setting | Value | Description |
|---------|-------|-------------|
| OTP Length | 6 digits | Numeric OTP |
| Expiry | 5 minutes | OTP validity period |
| Rate Limit | 3 requests / 5 min | Per email address |
| Max Attempts | 5 | Verification attempts per OTP |

---

## Common Error Codes

| Code | Message | Solution |
|------|---------|----------|
| 404 | User with this email does not exist | Ensure user exists |
| 429 | Too many OTP requests | Wait 5 minutes |
| 401 | Invalid OTP | Check OTP or request new one |
| 401 | OTP has expired | Request new OTP |
| 401 | Maximum verification attempts exceeded | Request new OTP |
| 500 | Failed to send OTP email | Check AWS SES configuration |

---

## Security Checklist

- ✅ OTPs are SHA-256 hashed before storage
- ✅ Rate limiting: 3 requests per 5 minutes
- ✅ Max 5 verification attempts per OTP
- ✅ OTP expires after 5 minutes
- ✅ Single-use OTPs (marked as used)
- ✅ User must exist (no auto-creation)
- ✅ Same JWT security as password login

---

## Troubleshooting

### Email Not Received
1. Check AWS SES sender email is verified
2. Check spam folder
3. Verify AWS credentials
4. Check AWS SES sandbox mode (can only send to verified emails)

### "Failed to send OTP email"
1. Verify AWS credentials in environment variables
2. Check AWS SES region matches configuration
3. Verify sender email is verified in AWS SES
4. Check AWS service status

### Database Issues
1. Ensure `otps` table exists
2. Check database connectivity
3. Verify indexes are created

---

## Related Files

- **Documentation**: `Document/OTP_AUTHENTICATION.md`
- **Controller**: `src/main/java/tiameds/com/tiameds/controller/auth/AuthController.java`
- **Service**: `src/main/java/tiameds/com/tiameds/services/auth/OtpService.java`
- **AWS Service**: `src/main/java/tiameds/com/tiameds/services/aws/AwsSesService.java`
- **Entity**: `src/main/java/tiameds/com/tiameds/entity/Otp.java`
- **Repository**: `src/main/java/tiameds/com/tiameds/repository/OtpRepository.java`

---

**For detailed documentation, see:** [OTP_AUTHENTICATION.md](./OTP_AUTHENTICATION.md)

