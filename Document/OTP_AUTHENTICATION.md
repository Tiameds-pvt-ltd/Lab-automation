# OTP-Based Authentication Documentation

## Overview

This document describes the OTP (One-Time Password) based authentication system integrated into the Lab Automation platform. The system now requires **both username/password authentication AND OTP verification** before issuing JWT tokens. This provides an additional layer of security through two-factor authentication (2FA).

## Table of Contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [API Endpoints](#api-endpoints)
4. [Configuration](#configuration)
5. [Security Features](#security-features)
6. [Database Schema](#database-schema)
7. [Setup Instructions](#setup-instructions)
8. [Usage Examples](#usage-examples)
9. [Troubleshooting](#troubleshooting)

---

## Features

### Core Functionality

- **Two-Step Authentication**: Requires username/password authentication followed by OTP verification
- **Email-based OTP**: Users receive a 4-digit numeric OTP via email after successful password authentication
- **Gmail SMTP Integration**: Uses Gmail SMTP via Spring Mail for reliable email delivery
- **Secure Storage**: OTPs are hashed using SHA-256 before storage
- **Automatic Expiration**: OTPs expire after 5 minutes
- **Rate Limiting**: Prevents abuse with configurable limits
- **Attempt Tracking**: Maximum 5 verification attempts per OTP
- **JWT Token Generation**: Tokens are ONLY generated after successful OTP verification
- **No Token Without OTP**: Login endpoint does NOT generate tokens - only sends OTP

### Security Features

- ✅ OTPs are never stored in plain text (SHA-256 hashed)
- ✅ Rate limiting: Max 3 OTP requests per 5 minutes per email
- ✅ Maximum 5 verification attempts per OTP
- ✅ OTP expiration: 5 minutes
- ✅ Single-use OTPs (marked as used after successful verification)
- ✅ User existence validation (no auto-creation)
- ✅ Same JWT token security as password-based login

---

## Architecture

### Components

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /auth/login (username + password)
       ▼
┌─────────────────┐
│ AuthController  │
└──────┬──────────┘
       │
       ├──► AuthenticationManager.authenticate()
       ├──► Fetch user from DB (extract email)
       ├──► OtpService.generateOtp()
       ├──► OtpService.saveOtp() (hashed)
       └──► EmailService.sendOtpEmail()
       │
       │ Returns: "OTP sent to your registered email"
       │ NO TOKENS GENERATED
       
       │ 2. POST /auth/verify-otp (email + OTP)
       ▼
┌─────────────────┐
│ AuthController  │
└──────┬──────────┘
       │
       ├──► OtpService.validateOtp()
       ├──► JwtUtil.generateAccessToken() ← Only after OTP success
       ├──► JwtUtil.generateRefreshToken() ← Only after OTP success
       └──► RefreshTokenService.saveRefreshToken()
       │
       │ Returns: Tokens + User data
```

### Flow Diagram

```
┌──────────┐                    ┌──────────┐
│  Client  │                    │  Server  │
└────┬─────┘                    └────┬─────┘
     │                               │
     │ 1. Login Request               │
     ├───────────────────────────────►│
     │  {username, password}         │
     │                               │
     │                               │ Authenticate credentials
     │                               │ Fetch user from DB
     │                               │ Extract email from DB
     │                               │ Generate 4-digit OTP
     │                               │ Hash OTP (SHA-256)
     │                               │ Store in database
     │                               │ Send email via Gmail SMTP
     │                               │
     │ 2. OTP Sent Response          │
     │◄──────────────────────────────┤
     │  {status: "success",          │
     │   message: "OTP sent..."}    │
     │  NO TOKENS                     │
     │                               │
     │ 3. Verify OTP Request         │
     ├───────────────────────────────►│
     │  {email, otp: "1234"}        │
     │                               │
     │                               │ Validate OTP
     │                               │ Check expiration
     │                               │ Check attempts
     │                               │ Verify hash
     │                               │ ✅ OTP Valid
     │                               │ Generate JWT tokens
     │                               │ Set cookies
     │                               │
     │ 4. Login Complete Response    │
     │◄──────────────────────────────┤
     │  {status: "success",          │
     │   data: {...user data...}}   │
     │  + Set-Cookie headers         │
     │  (accessToken, refreshToken)  │
     │                               │
```

### Key Classes

| Class | Responsibility |
|-------|---------------|
| `AuthController` | Handles OTP endpoints (`/auth/send-otp`, `/auth/verify-otp`) |
| `OtpService` | OTP generation, hashing, validation, rate limiting |
| `EmailService` | Email delivery via Gmail SMTP (Spring Mail) |
| `OtpRepository` | Database operations for OTP storage |
| `Otp` (Entity) | JPA entity representing OTP records |

---

## API Endpoints

### 1. Login (Username + Password → OTP)

**Endpoint:** `POST /api/v1/auth/login`

**Description:** Authenticates username and password, then generates and sends an OTP to the user's registered email address. **NO TOKENS ARE GENERATED** at this step.

**Request:**
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Request Validation:**
- `username` (required): Username or email address
- `password` (required): User's password

**Authentication Flow:**
1. Checks IP-based rate limiting (prevents brute force from same IP)
2. Checks user-based rate limiting (prevents brute force for same user)
3. Validates username and password using Spring Security
4. Fetches user from database (does not trust client input)
5. Extracts email from database (not from client)
6. Validates user has an email address
7. Checks OTP rate limiting (maximum 3 OTP requests per 5 minutes per email)
8. Generates 4-digit OTP
9. Saves hashed OTP to database
10. Sends OTP via Gmail SMTP to user's registered email
11. Returns success message **WITHOUT generating tokens**

**Response (Success - 200):**
```json
{
  "status": "success",
  "message": "OTP sent to your registered email",
  "data": {
    "email": "user@example.com",
    "message": "OTP sent to your registered email address"
  }
}
```

**Response (Error - 401):**
```json
{
  "status": "error",
  "message": "Incorrect username or password",
  "data": null
}
```

**Response (Error - 429):**
Possible messages:
- `"Too many login attempts from this IP address. Please try again later."` - IP rate limit exceeded
- `"Too many login attempts for this user. Please try again later."` - User rate limit exceeded
- `"Too many OTP requests. Please try again after 5 minutes."` - OTP rate limit exceeded

```json
{
  "status": "error",
  "message": "Too many OTP requests. Please try again after 5 minutes.",
  "data": null
}
```

**Response (Error - 400):**
```json
{
  "status": "error",
  "message": "User email not found. Please contact support.",
  "data": null
}
```

**Response (Error - 500):**
```json
{
  "status": "error",
  "message": "Failed to send OTP email. Please try again later.",
  "data": null
}
```

**Security Features:**
- **Multi-layer rate limiting:**
  - IP-based rate limiting (prevents brute force from same IP)
  - User-based rate limiting (prevents brute force for same user)
  - OTP request rate limiting: Maximum 3 OTP requests per 5 minutes per email
- Email is extracted from database, not from client input
- User email validation (ensures user has valid email before sending OTP)
- No tokens generated until OTP is verified
- Password authentication required before OTP is sent

---

### 2. Standalone OTP Sending (Optional)

**Endpoint:** `POST /api/v1/auth/send-otp`

**Description:** Standalone endpoint for sending OTP (useful for password reset or other use cases). For login flow, use `/auth/login` instead.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (Success - 200):**
```json
{
  "status": "success",
  "message": "OTP sent successfully",
  "data": {
    "email": "user@example.com",
    "message": "OTP has been sent to your email address"
  }
}
```

---

### 3. Verify OTP (Complete Login)

**Endpoint:** `POST /api/v1/auth/verify-otp`

**Description:** Validates the OTP and issues JWT access and refresh tokens upon successful verification. This is the **final step** of the login process.

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "1234"
}
```

**Request Validation:**
- `email` (required): Valid email address format
- `otp` (required): 4-digit numeric string (regex: `^\d{4}$`)

**Response (Success - 200):**
```json
{
  "status": "success",
  "message": "OTP verified successfully",
  "data": {
    "username": "john_doe",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ADMIN"],
    "modules": [...],
    "phone": "1234567890",
    "address": "123 Main St",
    "city": "City",
    "state": "State",
    "zip": "12345",
    "country": "Country",
    "enabled": true,
    "verified": true
  }
}
```

**Response Headers:**
```
Set-Cookie: accessToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=900; Path=/; HttpOnly; Secure; SameSite=Strict
Set-Cookie: refreshToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=Strict
```

**Response (Error - 401):**
```json
{
  "status": "error",
  "message": "Invalid OTP",
  "data": null
}
```

**Possible Error Messages:**
- `"OTP not found or expired"`
- `"OTP has expired"`
- `"OTP has already been used"`
- `"Maximum verification attempts exceeded"`
- `"Invalid OTP"`
- `"User with this email does not exist"`

**Verification Rules:**
- OTP must be valid and not expired (5-minute window)
- Maximum 5 verification attempts per OTP
- OTP is marked as used after successful verification
- User must exist in the database

---

## Configuration

### Environment Variables

The following environment variables must be configured for Gmail SMTP:

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SPRING_MAIL_HOST` | SMTP server host | No | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | SMTP server port | No | `587` |
| `SPRING_MAIL_USERNAME` | Gmail address for sending emails | Yes | - |
| `SPRING_MAIL_PASSWORD` | Gmail App Password (not regular password) | Yes | - |
| `SPRING_MAIL_SMTP_AUTH` | Enable SMTP authentication | No | `true` |
| `SPRING_MAIL_SMTP_STARTTLS_ENABLE` | Enable STARTTLS | No | `true` |

### Application Configuration

The Spring Mail configuration is defined in `application.yml` and profile-specific files:

**Base Configuration (`application.yml`):**
```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:yourgmail@gmail.com}
    password: ${SPRING_MAIL_PASSWORD:your_app_password}
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${SPRING_MAIL_SMTP_STARTTLS_ENABLE:true}
```

**Development Profile (`application-dev.yml`):**
```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:yourgmail@gmail.com}
    password: ${SPRING_MAIL_PASSWORD:your_app_password}
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${SPRING_MAIL_SMTP_STARTTLS_ENABLE:true}
```

**Test Profile (`application-test.yml`):**
```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:yourgmail@gmail.com}
    password: ${SPRING_MAIL_PASSWORD:your_app_password}
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${SPRING_MAIL_SMTP_STARTTLS_ENABLE:true}
```

**Production Profile (`application-prod.yml`):**
```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME}
    password: ${SPRING_MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${SPRING_MAIL_SMTP_STARTTLS_ENABLE:true}
```

### Gmail SMTP Setup

1. **Enable 2-Step Verification**:
   - Go to your Google Account settings
   - Navigate to Security → 2-Step Verification
   - Enable 2-Step Verification if not already enabled

2. **Generate App Password**:
   - Go to Google Account → Security → 2-Step Verification
   - Scroll down to "App passwords"
   - Click "App passwords"
   - Select "Mail" as the app and "Other" as the device
   - Enter a name (e.g., "Lab Automation")
   - Click "Generate"
   - **Copy the 16-character app password** (you won't see it again)

3. **Configure Environment Variables**:
   - Set `SPRING_MAIL_USERNAME` to your Gmail address (e.g., `yourgmail@gmail.com`)
   - Set `SPRING_MAIL_PASSWORD` to the 16-character app password (not your regular Gmail password)

4. **Important Notes**:
   - Use the **App Password**, not your regular Gmail password
   - App passwords are required when 2-Step Verification is enabled
   - Keep your app password secure and never commit it to version control
   - You can revoke app passwords at any time from Google Account settings

### OTP Configuration Constants

The following constants are defined in `OtpService`:

| Constant | Value | Description |
|----------|-------|-------------|
| `OTP_EXPIRY_MINUTES` | 5 | OTP expiration time in minutes |
| `MAX_OTP_REQUESTS_PER_WINDOW` | 3 | Maximum OTP requests per time window |
| `OTP_REQUEST_WINDOW_MINUTES` | 5 | Time window for rate limiting (minutes) |
| `MAX_VERIFICATION_ATTEMPTS` | 5 | Maximum verification attempts per OTP |

---

## Security Features

### 1. OTP Hashing

- **Algorithm**: SHA-256
- **Storage**: Only hashed OTPs are stored in the database
- **Verification**: Provided OTP is hashed and compared with stored hash
- **Benefit**: Even if database is compromised, raw OTPs cannot be extracted

### 2. Rate Limiting

- **Limit**: Maximum 3 OTP requests per 5 minutes per email
- **Purpose**: Prevents brute force attacks and email spam
- **Implementation**: Checks recent OTP requests in the database

### 3. Attempt Tracking

- **Limit**: Maximum 5 verification attempts per OTP
- **Tracking**: Each failed attempt increments the attempt counter
- **Benefit**: Prevents brute force OTP guessing

### 4. Expiration

- **Duration**: 5 minutes from creation
- **Validation**: Checked on every verification attempt
- **Cleanup**: Expired OTPs can be cleaned up via scheduled job

### 5. Single-Use OTPs

- **Behavior**: OTP is marked as `used = true` after successful verification
- **Benefit**: Prevents replay attacks

### 6. User Validation

- **Requirement**: Email must belong to an existing user
- **No Auto-Creation**: Users are not automatically created
- **Benefit**: Prevents unauthorized account creation

### 7. JWT Token Security

- **Same Security**: Uses identical JWT token generation as password login
- **Token Version**: Includes user's `tokenVersion` for instant revocation
- **Cookie Security**: Same HttpOnly, Secure, SameSite settings

---

## Database Schema

### OTP Table

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

-- Indexes for performance
CREATE INDEX idx_otp_email ON otps(email);
CREATE INDEX idx_otp_expires_at ON otps(expires_at);
```

### Table Columns

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key (auto-increment) |
| `email` | VARCHAR(255) | User's email address |
| `otp_hash` | VARCHAR(64) | SHA-256 hash of the OTP |
| `created_at` | TIMESTAMP | OTP creation timestamp |
| `expires_at` | TIMESTAMP | OTP expiration timestamp |
| `attempts` | INTEGER | Number of verification attempts |
| `used` | BOOLEAN | Whether OTP has been used |

### Indexes

- `idx_otp_email`: Speeds up queries by email
- `idx_otp_expires_at`: Speeds up expiration-based queries

---

## Setup Instructions

### 1. Database Migration

Run the following SQL to create the `otps` table:

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

**For Hibernate Auto-DDL (Development Only):**
If using `ddl-auto: update`, Hibernate will create the table automatically.

### 2. Gmail SMTP Configuration

1. **Set Environment Variables:**

   **Development:**
   ```bash
   export SPRING_MAIL_HOST=smtp.gmail.com
   export SPRING_MAIL_PORT=587
   export SPRING_MAIL_USERNAME=yourgmail@gmail.com
   export SPRING_MAIL_PASSWORD=your_16_char_app_password
   export SPRING_MAIL_SMTP_AUTH=true
   export SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
   ```

   **Test:**
   ```bash
   export SPRING_MAIL_HOST=smtp.gmail.com
   export SPRING_MAIL_PORT=587
   export SPRING_MAIL_USERNAME=yourgmail@gmail.com
   export SPRING_MAIL_PASSWORD=your_16_char_app_password
   export SPRING_MAIL_SMTP_AUTH=true
   export SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
   ```

   **Production:**
   ```bash
   export SPRING_MAIL_HOST=smtp.gmail.com
   export SPRING_MAIL_PORT=587
   export SPRING_MAIL_USERNAME=yourgmail@gmail.com
   export SPRING_MAIL_PASSWORD=your_16_char_app_password
   export SPRING_MAIL_SMTP_AUTH=true
   export SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
   ```

2. **Verify Gmail App Password**:
   - Ensure 2-Step Verification is enabled
   - Generate a new App Password if needed
   - Use the 16-character app password (not your regular password)

3. **Test Email Sending:**
   - Test the `/auth/send-otp` endpoint with a valid user email
   - Check spam folder if email doesn't arrive
   - Verify email is sent from your configured Gmail address

### 3. Application Startup

1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run with profile:**
   ```bash
   # Development
   java -jar app.jar --spring.profiles.active=dev
   
   # Test
   java -jar app.jar --spring.profiles.active=test
   
   # Production
   java -jar app.jar --spring.profiles.active=prod
   ```

3. **Verify OTP endpoints are accessible:**
   - Check logs for any SMTP connection errors
   - Test `/auth/send-otp` endpoint with a valid user email
   - Verify emails are being sent successfully

---

## Usage Examples

### Example 1: Complete Login Flow (Username + Password + OTP)

**Step 1: Login with Username and Password**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "OTP sent to your registered email",
  "data": {
    "email": "john@example.com",
    "message": "OTP sent to your registered email address"
  }
}
```

**Note:** No tokens are returned at this step. User must verify OTP to receive tokens.

**Step 2: Check Email**
- User receives email with subject: "Your Login OTP"
- Email contains: "Your login OTP is: 1234. Valid for 5 minutes."

**Step 3: Verify OTP**
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "otp": "1234"
  }' \
  -v
```

**Response:**
```json
{
  "status": "success",
  "message": "OTP verified successfully",
  "data": {
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    ...
  }
}
```

**Response Headers:**
```
Set-Cookie: accessToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=900; Path=/; HttpOnly; Secure; SameSite=Strict
Set-Cookie: refreshToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=Strict
```

**Cookies are set automatically in the response headers.**

### Example 2: JavaScript/Fetch API

```javascript
// Step 1: Login with username and password
async function login(username, password) {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username, password }),
  });
  const data = await response.json();
  return data; // Returns email and "OTP sent" message
}

// Step 2: Verify OTP
async function verifyOTP(email, otp) {
  const response = await fetch('/api/v1/auth/verify-otp', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // Important: include cookies
    body: JSON.stringify({ email, otp }),
  });
  const data = await response.json();
  return data; // Returns user data + tokens in cookies
}

// Complete login flow
async function completeLogin(username, password, otp) {
  // Step 1: Authenticate and get OTP
  const loginResponse = await login(username, password);
  if (loginResponse.status === 'error') {
    throw new Error(loginResponse.message);
  }
  
  // Extract email from response
  const email = loginResponse.data.email;
  
  // Step 2: Verify OTP (user enters OTP from email)
  const verifyResponse = await verifyOTP(email, otp);
  if (verifyResponse.status === 'error') {
    throw new Error(verifyResponse.message);
  }
  
  // Login complete - tokens are in cookies
  return verifyResponse.data;
}

// Usage
try {
  const userData = await completeLogin('john_doe', 'password123', '1234');
  console.log('Login successful:', userData);
} catch (error) {
  console.error('Login failed:', error.message);
}
```

### Example 3: Error Handling

```javascript
// Handle login errors
try {
  const loginResponse = await login('john_doe', 'password123');
  if (loginResponse.status === 'error') {
    // Handle errors:
    // - 401: Incorrect username or password
    // - 429: Too many OTP requests
    // - 500: Email sending failed
    console.error('Login error:', loginResponse.message);
    return;
  }
  
  // OTP sent successfully, prompt user for OTP
  const otp = prompt('Enter OTP sent to your email:');
  
  const verifyResponse = await verifyOTP(loginResponse.data.email, otp);
  if (verifyResponse.status === 'error') {
    // Handle errors:
    // - 401: Invalid OTP, expired, or max attempts exceeded
    // - 404: User not found
    console.error('OTP verification error:', verifyResponse.message);
    return;
  }
  
  // Success - tokens are in cookies
  console.log('Login successful!');
} catch (error) {
  console.error('Network error:', error);
}
```

---

## Troubleshooting

### Common Issues

#### 1. OTP Email Not Received

**Possible Causes:**
- Gmail App Password incorrect or expired
- Email in spam/junk folder
- 2-Step Verification not enabled
- Invalid SMTP credentials

**Solutions:**
- Verify Gmail App Password is correct (16 characters)
- Check spam folder
- Ensure 2-Step Verification is enabled in Google Account
- Verify `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` environment variables
- Generate a new App Password if needed

#### 2. "Failed to send OTP email" Error

**Possible Causes:**
- Gmail App Password incorrect
- SMTP connection issues
- Network connectivity problems
- Gmail account security restrictions

**Solutions:**
- Verify `SPRING_MAIL_PASSWORD` is the App Password (not regular password)
- Check `SPRING_MAIL_USERNAME` matches your Gmail address
- Ensure port 587 is not blocked by firewall
- Check Gmail account for security alerts
- Try generating a new App Password

#### 3. "User with this email does not exist"

**Cause:** Email doesn't match any user in the database

**Solution:** Ensure user exists before requesting OTP

#### 4. "Too many OTP requests"

**Cause:** Rate limit exceeded (3 requests per 5 minutes)

**Solution:** Wait 5 minutes before requesting another OTP

#### 5. "Maximum verification attempts exceeded"

**Cause:** More than 5 failed verification attempts

**Solution:** Request a new OTP

#### 6. "OTP has expired"

**Cause:** OTP expired (5 minutes elapsed)

**Solution:** Request a new OTP

#### 7. Database Connection Issues

**Symptoms:** OTP not saved or validation fails

**Solutions:**
- Check database connectivity
- Verify `otps` table exists
- Check database logs for errors

### Debugging

#### Enable Debug Logging

Add to `application-dev.yml`:
```yaml
logging:
  level:
    tiameds.com.tiameds.services.auth.OtpService: DEBUG
    tiameds.com.tiameds.services.email.EmailService: DEBUG
    org.springframework.mail: DEBUG
```

#### Check OTP in Database

```sql
SELECT email, created_at, expires_at, attempts, used 
FROM otps 
WHERE email = 'user@example.com' 
ORDER BY created_at DESC;
```

#### Verify SMTP Configuration

```bash
# Test SMTP connection (using telnet or similar tool)
telnet smtp.gmail.com 587

# Or use curl to test (if mail server supports it)
# Note: This is just for connection testing, not actual email sending
```

### Logs to Monitor

- `OtpService`: OTP generation, validation, rate limiting
- `EmailService`: Email sending success/failure
- `AuthController`: Endpoint requests and responses
- `org.springframework.mail`: SMTP connection and email delivery logs

---

## Best Practices

### 1. Email Delivery

- Use a dedicated Gmail account for sending OTP emails
- Monitor email delivery rates and check spam reports
- Keep Gmail App Password secure and rotate periodically
- Consider using a Gmail Workspace account for production (better limits)
- Monitor Gmail account for security alerts

### 2. Security

- Never log raw OTPs
- Rotate Gmail App Passwords regularly
- Never commit App Passwords to version control
- Monitor for suspicious OTP request patterns
- Implement IP-based rate limiting (future enhancement)
- Use environment variables or secret management for credentials

### 3. Performance

- Clean up expired OTPs periodically (scheduled job)
- Monitor database query performance
- Consider Redis for OTP storage in high-traffic scenarios

### 4. User Experience

- Provide clear error messages
- Show countdown timer for OTP expiration
- Allow resending OTP after rate limit window
- Support OTP copy-paste in UI

---

## Future Enhancements

### Potential Improvements

1. **SMS OTP Support**: Add SMS-based OTP delivery via AWS SNS
2. **Redis Storage**: Use Redis for faster OTP storage/retrieval
3. **IP-Based Rate Limiting**: Additional rate limiting by IP address
4. **OTP Resend**: Allow resending OTP before expiration
5. **OTP History**: Track OTP usage history for audit
6. **Custom Email Templates**: Support for custom email templates
7. **Multi-Factor Authentication**: Combine OTP with password for MFA

---

## Related Documentation

- [JWT Authentication Overview](./JWT_AUTH_OVERVIEW.md) - JWT token system documentation
- [API Documentation](./API_DOCUMENTATION.md) - Complete API reference (if available)

---

## Support

For issues or questions:
1. Check this documentation
2. Review application logs
3. Verify Gmail SMTP configuration and App Password
4. Contact the development team

---

**Last Updated:** 2024
**Version:** 2.0.0
**Changes:** Migrated from AWS SES to Gmail SMTP for email delivery

