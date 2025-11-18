# Production-Ready Onboarding Flow Documentation

## Overview

This document describes the complete database-backed onboarding flow for the Lab Automation system. The flow is designed to be production-ready, secure, and scalable across multiple instances.

## Architecture

### Flow Diagram

```
1. User enters email
   ↓
2. POST /public/onboarding/request-verification
   ↓
3. System checks DB rate limit (max 3 emails/hour)
   ↓
4. Generate secure token (hashed in DB)
   ↓
5. Send verification email with link
   ↓
6. User clicks link → GET /public/onboarding/verify-email?token=xxx
   ↓
7. Validate token from DB (not consumed yet)
   ↓
8. Redirect to onboarding form with token
   ↓
9. User completes form → POST /public/onboarding/complete
   ↓
10. Validate & consume token (single-use enforcement)
   ↓
11. Create User account + Lab in transaction
   ↓
12. Account activated
```

## Database Schema

### `verification_tokens` Table

```sql
CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_identifier VARCHAR(16) NOT NULL UNIQUE,
    expiry_time TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP
);

CREATE INDEX idx_verification_token_email ON verification_tokens(email);
CREATE INDEX idx_verification_token_hash ON verification_tokens(token_hash);
CREATE INDEX idx_verification_token_identifier ON verification_tokens(token_identifier);
CREATE INDEX idx_verification_token_expiry ON verification_tokens(expiry_time);
CREATE INDEX idx_verification_token_created ON verification_tokens(created_at);
```

**Fields:**
- `id`: Primary key
- `email`: Email address for which token was generated
- `token_hash`: SHA-256 hash of the full token (stored securely, like a password)
- `token_identifier`: First 16 characters of token (for fast lookup)
- `expiry_time`: Token expiration timestamp (default: 15 minutes)
- `used`: Boolean flag indicating if token has been consumed
- `created_at`: Token creation timestamp
- `used_at`: Timestamp when token was consumed

## Security Features

### 1. Secure Token Generation
- **256-bit cryptographically secure random tokens**
- Tokens are hashed using BCrypt (same as passwords) before storage
- Plain token exists only in memory and email - never stored in DB
- Two-part token: identifier (16 chars) + verification code (remaining)

### 2. Single-Use Enforcement
- Tokens are marked as `used=true` immediately upon validation
- Transactional enforcement ensures atomicity
- Once used, token can never be reused

### 3. Expiration
- Default: 15 minutes (configurable via `onboarding.token.expiry-minutes`)
- Expired tokens are automatically rejected
- Expiry checked on every validation

### 4. Database-Based Rate Limiting
- **No in-memory cache** - fully database-backed
- Works across multiple instances (multi-instance deployment ready)
- Default: Max 3 emails per hour per email address
- Configurable via `onboarding.rate-limit.max-emails-per-hour`
- Returns HTTP 429 (Too Many Requests) when limit exceeded

### 5. No Open Redirects
- Frontend URLs are configured server-side
- Token validation ensures redirects only to configured URLs
- No user-controlled redirect parameters

## API Endpoints

### 1. Request Verification Email

**Endpoint:** `POST /api/v1/public/onboarding/request-verification`

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success - 200):**
```json
{
  "status": "success",
  "message": "Verification email sent successfully. Please check your inbox.",
  "data": null
}
```

**Response (Rate Limited - 429):**
```json
{
  "status": "error",
  "message": "Too many verification emails sent. Please try again after 60 minutes. Remaining attempts: 0"
}
```

**Flow:**
1. Validates email format
2. Checks database rate limit
3. Generates secure token and stores hash
4. Sends verification email
5. Returns success response

---

### 2. Verify Email Token

**Endpoint:** `GET /api/v1/public/onboarding/verify-email?token=<token>`

**Response (Valid - 200):**
```json
{
  "valid": true,
  "email": "user@example.com",
  "message": "Email verified successfully. Please complete the onboarding form.",
  "redirectUrl": "http://localhost:3000/onboarding?token=<encoded-token>"
}
```

**Response (Invalid - 400):**
```json
{
  "status": "error",
  "message": "Invalid or expired verification token. Please request a new verification email."
}
```

**Flow:**
1. Extracts token identifier (first 16 chars)
2. Looks up token in database
3. Validates token (not used, not expired, hash matches)
4. Returns redirect URL to onboarding form
5. **Token is NOT consumed here** - consumed only on form submission

---

### 3. Complete Onboarding

**Endpoint:** `POST /api/v1/public/onboarding/complete`

**Request Body:**
```json
{
  "token": "<verification-token>",
  "email": "user@example.com",
  "username": "johndoe",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zip": "10001",
  "country": "USA",
  "lab": {
    "name": "ABC Medical Lab",
    "address": "456 Lab St",
    "city": "New York",
    "state": "NY",
    "description": "Full-service medical laboratory",
    "licenseNumber": "LAB123456",
    "labType": "Clinical",
    "labZip": "10001",
    "labCountry": "USA",
    "labPhone": "+1234567890",
    "labEmail": "info@abclab.com",
    "directorName": "Dr. Jane Smith",
    "directorEmail": "director@abclab.com",
    "directorPhone": "+1234567890",
    "dataPrivacyAgreement": true
  }
}
```

#### Mandatory Lab Fields
The `labs` table marks the following columns as `NOT NULL`. The onboarding form **must** supply values for each of these fields or the transaction will fail with a `DataIntegrityViolationException`:

| DTO field (`lab.*`)        | Database column              | Notes |
|---------------------------|------------------------------|-------|
| `name`                    | `name`                       | Required |
| `address`                 | `address`                    | Required |
| `city`                    | `city`                       | Required |
| `state`                   | `state`                      | Required |
| `description`             | `description`                | Required |
| `licenseNumber`           | `license_number`             | Required |
| `labType`                 | `lab_type`                   | Required |
| `labZip`                  | `lab_zip`                    | Required |
| `labCountry`              | `lab_country`                | Required |
| `labPhone`                | `lab_phone`                  | Required |
| `labEmail`                | `lab_email`                  | Required |
| `directorName`            | `director_name`              | Required |
| `directorEmail`           | `director_email`             | Required |
| `directorPhone`           | `director_phone`             | Required |
| `certificationBody`       | `certification_body`         | Required |
| `labCertificate`          | `lab_certificate`            | Required |
| `directorGovtId`          | `director_govt_id`           | Required |
| `labBusinessRegistration` | `lab_business_registration`  | Required |
| `labLicense`              | `lab_license`                | Required |
| `taxId`                   | `tax_id`                     | Required |
| `labAccreditation`        | `lab_accreditation`          | Required |
| `dataPrivacyAgreement`    | `data_privacy_agreement`     | Required (boolean) |

`labLogo` is the only optional onboarding field; it maps to `lab_logo` which is nullable in the entity.

If your onboarding UX cannot capture some of these values yet, either:  
1. Mark them as optional in the database (set `nullable = true` and update the schema), **or**  
2. Provide sensible defaults/placeholder values before persisting the `Lab`.

**Response (Success - 201):**
```json
{
  "status": "success",
  "message": "Onboarding completed successfully",
  "data": {
    "userId": 123,
    "username": "johndoe",
    "email": "user@example.com",
    "labId": 456,
    "labName": "ABC Medical Lab",
    "accountActive": true
  }
}
```

**Response (Invalid Token - 400):**
```json
{
  "status": "error",
  "message": "Invalid or expired verification token"
}
```

**Flow:**
1. Validates and **consumes** token (marks as used)
2. Verifies email matches token email
3. Checks if user/email already exists
4. Creates User account (in transaction)
5. Creates Lab (organization)
6. Associates user with lab
7. Activates account
8. Returns success response

**Transaction:** All steps are wrapped in `@Transactional` for atomicity.

---

### 4. Resend Verification Email

**Endpoint:** `POST /api/v1/public/onboarding/resend-verification`

Same as request-verification endpoint. Subject to same rate limiting.

## Configuration

Add to `application.yml`:

```yaml
onboarding:
  token:
    expiry-minutes: 15  # Token expiration (default: 15)
  rate-limit:
    max-emails-per-hour: 3  # Max emails per hour (default: 3)
    window-minutes: 60  # Rate limit window (default: 60)
  frontend:
    base-url: http://localhost:3000
    verification-url: http://localhost:3000/verify-email
    onboarding-url: http://localhost:3000/onboarding
```

## Folder Structure

```
src/main/java/tiameds/com/tiameds/
├── entity/
│   └── VerificationToken.java          # Token entity
├── repository/
│   └── VerificationTokenRepository.java # Token repository
├── dto/
│   └── onboarding/
│       ├── EmailRequestDTO.java
│       ├── VerificationResponseDTO.java
│       ├── OnboardingRequestDTO.java
│       └── OnboardingResponseDTO.java
├── services/
│   ├── onboarding/
│   │   ├── VerificationTokenService.java    # Token generation & validation
│   │   ├── EmailRateLimitService.java       # DB-based rate limiting
│   │   └── OnboardingService.java           # User & Lab creation
│   └── email/
│       └── EmailService.java                 # Email sending (updated)
└── controller/
    └── onboarding/
        ├── AuthOnboardingController.java     # Email request endpoint
        ├── EmailVerificationController.java  # Token validation endpoint
        └── OnboardingController.java         # Form submission endpoint
```

## Best Practices Implemented

### ✅ Security
- [x] Long random tokens (256 bits)
- [x] Tokens hashed before storage (BCrypt)
- [x] Single-use enforcement
- [x] Expiration time (15 minutes)
- [x] Transaction wrapping for token consumption
- [x] No open redirects
- [x] Proper HTTP status codes

### ✅ Database Design
- [x] Token identifier for fast lookup
- [x] Indexes on all query fields
- [x] Rate limiting via DB queries (no cache)
- [x] Transactional operations

### ✅ Scalability
- [x] Database-only rate limiting (multi-instance ready)
- [x] Efficient token lookup (identifier-based)
- [x] No in-memory state

### ✅ Code Quality
- [x] Clean controller separation
- [x] Service layer abstraction
- [x] Comprehensive error handling
- [x] Logging throughout
- [x] Input validation
- [x] Swagger documentation

## Testing Recommendations

### Unit Tests
- Token generation and hashing
- Token validation logic
- Rate limiting calculations
- Email sending (mocked)

### Integration Tests
- Complete onboarding flow
- Rate limit enforcement
- Token expiration
- Single-use enforcement
- Transaction rollback on errors

### Security Tests
- Token reuse attempts
- Expired token usage
- Rate limit bypass attempts
- SQL injection (via JPA - should be safe)
- XSS in email content (should be escaped)

## Production Deployment Checklist

- [ ] Update `onboarding.frontend.*` URLs to production frontend
- [ ] Configure email SMTP settings
- [ ] Set appropriate token expiry (15 minutes recommended)
- [ ] Configure rate limits (3 per hour recommended)
- [ ] Enable database connection pooling
- [ ] Set up monitoring for:
  - Token generation rate
  - Email sending failures
  - Rate limit hits
  - Onboarding completion rate
- [ ] Schedule cleanup job for expired tokens (optional)
- [ ] Test email delivery
- [ ] Test complete flow end-to-end
- [ ] Load test rate limiting

## Maintenance

### Cleanup Expired Tokens

Optional scheduled job to clean up old tokens:

```java
@Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
public void cleanupExpiredTokens() {
    Instant now = Instant.now();
    tokenRepository.deleteExpiredTokens(now);
}
```

## Troubleshooting

### Email Not Received
1. Check SMTP configuration
2. Check spam folder
3. Verify email address
4. Check application logs

### Token Invalid
1. Check token expiration (15 minutes)
2. Verify token hasn't been used
3. Check token format (should be URL-encoded)
4. Verify token identifier exists in DB

### Rate Limit Issues
1. Check `verification_tokens` table for email
2. Verify rate limit configuration
3. Check time window calculation

## Support

For issues or questions, contact the development team or refer to the main project documentation.

