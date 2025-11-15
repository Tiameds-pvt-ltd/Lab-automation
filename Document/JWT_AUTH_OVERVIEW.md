# JWT Authentication Overview

This document provides a comprehensive explanation of how JSON Web Tokens (JWT) are issued, validated, and revoked in the Lab Automation platform using RS256-based asymmetric cryptography with refresh token rotation.

## Token Issuance

### Key Generation and Loading
- **RSA Key Pair**: The system uses RS256 (RSA Signature with SHA-256) for signing JWTs, providing asymmetric cryptography where:
  - **Private Key**: Used to sign tokens (loaded from `security.jwt.private-key-location`)
  - **Public Key**: Used to verify token signatures (loaded from `security.jwt.public-key-location`)
- **Initialization**: `JwtUtil.initKeys()` loads the RSA key pair at application startup using `@PostConstruct`, parsing PEM-encoded keys (PKCS8 for private, X509 for public).
- **Security**: The private key never leaves the server; only the public key is needed for verification, making token validation secure even if the public key is exposed.

### Token Generation Process
1. **Entry Point**: `AuthController.login()` authenticates user credentials via Spring Security's `AuthenticationManager`.
2. **Token Creation**: After successful authentication, `JwtUtil` generates both tokens:
   - **Access Token**: Short-lived token (default 15 minutes) for API requests
   - **Refresh Token**: Long-lived token (default 24 hours) for obtaining new access tokens
3. **Claims Structure**:
   - **Access Token Claims**:
     - `sub` (subject): Username
     - `tokenVersion`: Current user token version (for revocation)
     - `tokenType`: "access"
     - `iss` (issuer): Configured issuer value
     - `aud` (audience): Configured audience value
     - `iat` (issued at): Token creation timestamp
     - `exp` (expiration): Token expiration timestamp
   - **Refresh Token Claims**: Same as access token, plus:
     - `jti` (JWT ID): Unique UUID that maps to a persisted `RefreshToken` entity in the database
4. **Signing**: Both tokens are signed with the RSA private key using RS256 algorithm.
5. **Storage**: 
   - Access tokens are stateless (no database storage)
   - Refresh tokens are hashed using SHA-256 and stored in the `refresh_tokens` table with:
     - Token ID (UUID from `jti` claim)
     - User reference
     - SHA-256 hash of the token value
     - Expiration timestamp
     - Revocation status

### Token Rotation Strategy
- **On Login**: A new refresh token is generated and stored; any existing active refresh tokens for the user remain valid (multiple devices/sessions supported).
- **On Refresh**: The presented refresh token is immediately revoked, and a new refresh token is issued. This ensures:
  - Only one refresh token can be used at a time per session
  - Compromised refresh tokens have limited window of abuse
  - Token rotation prevents token reuse attacks

## End-to-End Authentication Flow

### 1. Login (`POST /auth/login`)

**Request Flow**:
1. Client sends credentials in request body
2. Rate limiting checks (IP-based and user-based) prevent brute force attacks
3. Spring Security `AuthenticationManager` validates credentials
4. On success, user details are loaded and tokens are generated

**Request Body**:
```json
{
  "username": "Sunny3040",
  "password": "••••••••"
}
```

**Successful Response** (HTTP 200):
```json
{
  "status": "success",
  "message": "Login successful",
  "data": {
    "username": "Sunny3040",
    "email": "Sunny3040@gmail.com",
    "firstName": "Sunny",
    "lastName": "Sunny",
    "roles": ["SUPERADMIN"],
    "modules": [...],
    "phone": "9473337583",
    "address": "29287SN27247thmainroad4972",
    "city": "MYSORE",
    "state": "Karnataka",
    "zip": "570017",
    "country": "India",
    "enabled": true,
    "verified": false
  }
}
```

**Response Headers** (Set-Cookie):
```
Set-Cookie: accessToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=900; Path=/; HttpOnly; Secure; SameSite=Strict
Set-Cookie: refreshToken=eyJhbGciOiJSUzI1NiJ9...; Max-Age=86400; Path=/; HttpOnly; Secure; SameSite=Strict
```

**Security Features**:
- **HTTP-only cookies**: JavaScript cannot access tokens, preventing XSS attacks
- **Secure flag**: Cookies only sent over HTTPS (in production)
- **SameSite=Strict**: Prevents CSRF attacks by not sending cookies on cross-site requests
- **Token storage**: Refresh token hash is stored in database for validation and revocation

### 2. Authenticating API Calls

**Token Resolution** (in order of precedence):
1. **Cookie**: `JwtFilter` first checks for `accessToken` cookie (automatic in browsers)
2. **Authorization Header**: Falls back to `Authorization: Bearer <accessToken>` header (for non-browser clients)

**Validation Process** (`JwtFilter.doFilterInternal`):
1. **Token Extraction**: Resolves token from cookie or Authorization header
2. **JWT Parsing**: `JwtUtil.parseAccessToken()` validates:
   - **Signature**: Verifies RS256 signature using public key
   - **Issuer**: Ensures `iss` claim matches configured issuer
   - **Audience**: Ensures `aud` claim matches configured audience
   - **Token Type**: Validates `tokenType` claim equals "access"
   - **Expiration**: Checks `exp` claim is not in the past
3. **User Lookup**: Retrieves user from database using `sub` (username) claim
4. **Token Version Check**: Compares `tokenVersion` claim with `User.tokenVersion` in database
   - **Purpose**: Invalidates all tokens after logout or security events
   - **If mismatch**: Request is rejected with 401 Unauthorized
5. **Security Context**: On success, creates `UsernamePasswordAuthenticationToken` and sets it in `SecurityContextHolder`
   - Enables Spring Security authorization checks (roles, permissions)
   - Available to controllers via `@AuthenticationPrincipal` or `Authentication` parameter

**Public Endpoints** (bypass JWT filter):
- `/auth/login`
- `/auth/refresh`
- `/public/login`
- `/public/register`
- OPTIONS requests (CORS preflight)

### 3. Refresh (`POST /auth/refresh`)

**Request Requirements**:
- Must include `refreshToken` cookie (no request body needed)
- No authentication required (public endpoint)

**Validation Process**:
1. **Token Extraction**: Reads `refreshToken` from cookies
2. **JWT Validation**: `JwtUtil.parseRefreshToken()` validates:
   - RS256 signature
   - Issuer and audience claims
   - Token type equals "refresh"
   - Expiration not passed
3. **Claims Extraction**: Extracts `sub` (username), `tokenVersion`, and `jti` (token ID)
4. **User Validation**: 
   - Verifies user exists in database
   - Checks `tokenVersion` claim matches database value
5. **Refresh Token Validation**: `RefreshTokenService.validateToken()`:
   - Looks up refresh token by `jti` (UUID) in database
   - Verifies token is not revoked
   - Verifies token has not expired (double-check)
   - **Hash Verification**: Computes SHA-256 hash of presented token and compares with stored hash
     - **Security**: Even if database is compromised, raw tokens cannot be extracted from hashes
6. **Token Rotation**:
   - **Revoke Old Token**: Marks current refresh token as revoked in database
   - **Generate New Tokens**: Creates new access and refresh tokens
   - **Store New Refresh Token**: Saves hashed refresh token to database
   - **Set Cookies**: Returns new tokens in Set-Cookie headers

**Successful Response** (HTTP 200):
```json
{
  "status": "success",
  "message": "Token refreshed successfully",
  "data": {
    "username": "Sunny3040",
    "email": "Sunny3040@gmail.com",
    ...
  }
}
```

**Failure Scenarios** (HTTP 401/403):
- Missing refresh token cookie
- Invalid/expired refresh token
- Token version mismatch (user logged out or token rotated)
- Refresh token revoked
- Hash mismatch (token tampering detected)
- User not found

**On Failure**: Both cookies are cleared (`Max-Age=0`) and client must re-authenticate via login.

### 4. Logout (`POST /auth/logout`)

**Request Requirements**:
- Must be authenticated (valid access token required)
- Access token extracted from cookie or Authorization header

**Logout Process**:
1. **Authentication Check**: Verifies user is authenticated via `Authentication` parameter
2. **User Lookup**: Retrieves user from database
3. **Refresh Token Revocation** (if present):
   - Extracts `refreshToken` from cookies
   - Parses token to get `jti` (token ID)
   - Marks refresh token as revoked in database
   - Handles expired tokens gracefully (still revokes if possible)
4. **Token Version Increment**: 
   - Increments `User.tokenVersion` in database
   - **Critical Security Feature**: Invalidates ALL existing access tokens instantly
   - Even if tokens are not expired, they become invalid due to version mismatch
5. **Cookie Clearing**: Sets both cookies with `Max-Age=0` to remove them from client

**Successful Response** (HTTP 200):
```json
{
  "status": "success",
  "message": "Logged out successfully",
  "data": {
    "tokenVersion": 25
  }
}
```

**Security Impact**:
- All access tokens for the user become invalid immediately (version mismatch)
- Refresh token is revoked (cannot be used to get new access tokens)
- Client cookies are cleared
- User must re-authenticate to obtain new tokens

### 5. Error Handling
- Invalid credentials → `401` with `status: "error"` and throttling may trigger `429`.
- Expired access token but valid refresh token → call `/auth/refresh`.
- Expired or revoked refresh token → cookies cleared, client must re-authenticate via `/auth/login`.

## Configuration
- **Key management**:
  - `security.jwt.private-key-location` and `security.jwt.public-key-location` define where the PEM-encoded RSA keys live. The dev profile ships with sample classpath keys (`src/main/resources/keys/`). Higher environments should use `file:` URLs that point to secrets outside the repo.
  - Additional `security.jwt.*` properties control issuer, audience, token TTLs, cookie names, domain/path, secure flag, and SameSite policy.
- **Stateless access**: `SpringSecurityConfig` registers `JwtFilter` ahead of the username/password filter and enforces `SessionCreationPolicy.STATELESS`, so every request must present a valid access token.
- **Cookie storage**: Access and refresh tokens are returned as HTTP-only cookies (defaults `accessToken` / `refreshToken`). JavaScript never handles the raw token strings; header-based Bearer tokens remain an optional fallback for legacy clients.

## Request Validation Flow (Detailed)

### Step-by-Step Process

1. **Request Interception**: 
   - `JwtFilter` extends `OncePerRequestFilter` and runs before Spring Security's authentication chain
   - Checks `shouldNotFilter()` to skip public endpoints (`/auth/login`, `/auth/refresh`, etc.)

2. **Token Resolution**:
   - `resolveAccessToken()` method:
     - First checks cookies for `accessToken` (configured cookie name)
     - Falls back to `Authorization: Bearer <token>` header
     - Returns `null` if neither found (request continues unauthenticated)

3. **JWT Parsing and Validation**:
   - `JwtUtil.parseAccessToken(token)` performs:
     - **Base Parsing**: `parseClaims()` uses JJWT library to:
       - Parse JWT structure (header.payload.signature)
       - Verify RS256 signature using public key
       - Validate `iss` (issuer) matches configuration
       - Validate `aud` (audience) matches configuration
     - **Token Type Check**: `ensureTokenType()` validates `tokenType` claim equals "access"
     - **Expiration Check**: `ensureNotExpired()` validates `exp` claim is in the future
     - Returns `Claims` object on success, throws exception on failure

4. **User and Version Validation**:
   - Extracts `sub` (subject/username) from claims
   - Loads `User` entity from database
   - Extracts `tokenVersion` from claims
   - Compares claim `tokenVersion` with `User.tokenVersion` from database
   - **If mismatch**: Returns 401 Unauthorized (token invalidated by logout/security event)

5. **Security Context Setup**:
   - Loads `UserDetails` via `UserDetailsService`
   - Creates `UsernamePasswordAuthenticationToken` with:
     - Principal: `UserDetails`
     - Credentials: `null` (not needed after JWT validation)
     - Authorities: User roles/permissions from `UserDetails`
   - Sets authentication in `SecurityContextHolder.getContext()`

6. **Request Continuation**:
   - Filter chain continues
   - Spring Security authorization checks can now access authenticated user
   - Controllers can inject `Authentication` or use `@AuthenticationPrincipal`

### Error Handling
- **Expired Token**: `ExpiredJwtException` → 401 with "Access token has expired"
- **Invalid Token**: `JwtException` or `IllegalArgumentException` → 401 with "Invalid access token"
- **User Not Found**: 401 with "User associated with token not found"
- **Version Mismatch**: 401 with "Token version mismatch"
- All errors clear `SecurityContext` and return JSON error response

## Refresh Flow (Detailed)

### Complete Refresh Token Lifecycle

1. **Token Presentation**:
   - Client sends `POST /auth/refresh` with `refreshToken` cookie
   - No request body required
   - No authentication required (public endpoint)

2. **Initial Validation**:
   - Extracts refresh token from cookie
   - `JwtUtil.parseRefreshToken()` validates:
     - JWT structure and RS256 signature
     - Issuer and audience claims
     - Token type equals "refresh"
     - Expiration not passed
   - Extracts claims: `sub` (username), `tokenVersion`, `jti` (token ID)

3. **User Validation**:
   - Loads user from database using `sub`
   - Verifies user exists
   - Compares `tokenVersion` claim with database value
   - **If mismatch**: Token invalidated (user logged out or security event)

4. **Database Token Validation** (`RefreshTokenService.validateToken`):
   - Looks up `RefreshToken` entity by `jti` (UUID) in database
   - **Checks**:
     - Token exists in database
     - `revoked` flag is `false`
     - `expiresAt` is in the future (double-check expiration)
     - **Hash Verification**: 
       - Computes SHA-256 hash of presented token: `MessageDigest.getInstance("SHA-256")`
       - Compares with stored `tokenHash` in database
       - **Security**: Prevents token tampering and ensures token matches stored record
   - Returns `RefreshToken` entity on success

5. **Token Rotation**:
   - **Revoke Old Token**: `refreshTokenService.revokeToken()` sets `revoked = true`
   - **Generate New Tokens**:
     - New access token (15 minutes TTL)
     - New refresh token (24 hours TTL) with new UUID `jti`
   - **Store New Refresh Token**: 
     - Computes SHA-256 hash of new refresh token
     - Saves to database with new UUID, user reference, expiration, and `revoked = false`

6. **Response**:
   - Returns login response format with user data
   - Sets new `accessToken` and `refreshToken` cookies
   - Old refresh token cookie is overwritten

### Security Benefits of Token Rotation
- **Single-Use Refresh Tokens**: Each refresh token can only be used once
- **Compromise Detection**: If old token is used after rotation, it's detected (revoked)
- **Limited Attack Window**: Compromised refresh tokens have limited lifetime
- **Database Tracking**: All refresh tokens are tracked and can be revoked individually

## Token Versioning and Revocation Strategy

### Token Version Mechanism

**Purpose**: Provides instant token invalidation without maintaining a blacklist of tokens.

**How It Works**:
- Each `User` entity has a `tokenVersion` integer field (starts at 0, increments on logout/security events)
- Every JWT (access and refresh) includes `tokenVersion` in its claims
- On every request, `JwtFilter` compares token's `tokenVersion` claim with database `User.tokenVersion`
- **If mismatch**: Token is rejected (user logged out or security event occurred)

**Benefits**:
- **Stateless Access Tokens**: No need to store access tokens in database
- **Instant Invalidation**: All tokens become invalid immediately when version increments
- **Scalable**: Works across multiple servers without shared state
- **Efficient**: Single integer comparison per request

### Logout Process

1. **Refresh Token Revocation**: 
   - Attempts to parse and revoke refresh token from cookie
   - Handles expired/invalid tokens gracefully (still proceeds with logout)

2. **Version Increment**:
   - `user.setTokenVersion(user.getTokenVersion() + 1)`
   - Persists to database
   - **Effect**: All existing access tokens become invalid (version mismatch)

3. **Cookie Clearing**:
   - Sets both cookies with `Max-Age=0`
   - Client removes cookies from browser

### Global Revocation Scenarios

**Admin Actions**:
- Admin can increment `User.tokenVersion` to invalidate all user sessions
- Useful for security incidents, password changes, or forced logout

**Bulk Revocation**:
- `RefreshTokenService.revokeAllActiveTokens(userId)`:
  - Finds all active (non-revoked) refresh tokens for user
  - Marks all as `revoked = true`
  - Combined with version increment, invalidates all sessions

**Use Cases**:
- Password change requiring re-authentication
- Security breach detection
- Account suspension
- Admin-initiated logout

## Security Architecture Summary

### Token Types and Characteristics

**Access Tokens**:
- **Algorithm**: RS256 (RSA Signature with SHA-256)
- **Lifetime**: 15 minutes (configurable via `security.jwt.access-token-ttl`)
- **Storage**: HTTP-only cookies (prevents XSS attacks)
- **Validation**: Stateless (no database lookup required)
- **Claims**: `sub`, `tokenVersion`, `tokenType`, `iss`, `aud`, `iat`, `exp`
- **Revocation**: Via `tokenVersion` increment (instant invalidation)

**Refresh Tokens**:
- **Algorithm**: RS256 (same as access tokens)
- **Lifetime**: 24 hours (configurable via `security.jwt.refresh-token-ttl`)
- **Storage**: HTTP-only cookies + database (hashed)
- **Validation**: Stateful (database lookup and hash verification required)
- **Claims**: Same as access tokens + `jti` (UUID)
- **Revocation**: Database flag (`revoked = true`) + token rotation

### Security Mechanisms

1. **Asymmetric Cryptography (RS256)**:
   - Private key never exposed (only signs tokens)
   - Public key can be shared (only verifies signatures)
   - Prevents token forgery without private key

2. **Token Versioning**:
   - Instant invalidation of all tokens on logout/security events
   - No token blacklist required (stateless for access tokens)
   - Efficient single-integer comparison per request

3. **Refresh Token Hashing**:
   - Raw tokens never stored in database (only SHA-256 hashes)
   - Even database compromise doesn't expose tokens
   - Hash verification prevents token tampering

4. **Token Rotation**:
   - Refresh tokens are single-use (rotated on every refresh)
   - Compromised tokens have limited attack window
   - Old tokens automatically revoked

5. **Cookie Security**:
   - HTTP-only: Prevents JavaScript access (XSS protection)
   - Secure: HTTPS-only in production (prevents interception)
   - SameSite=Strict: CSRF protection

6. **Rate Limiting**:
   - IP-based and user-based rate limiting on login
   - Prevents brute force attacks

### Configuration Requirements

**Production Checklist**:
- ✅ RSA key pair generated and stored securely (outside repository)
- ✅ Private key location configured (`security.jwt.private-key-location`)
- ✅ Public key location configured (`security.jwt.public-key-location`)
- ✅ Cookie `secure` flag enabled for HTTPS
- ✅ Appropriate `SameSite` policy configured
- ✅ Token TTLs configured appropriately
- ✅ Issuer and audience values set correctly

**Key Files**:
- `JwtUtil.java`: Token generation and parsing
- `JwtFilter.java`: Request authentication
- `AuthController.java`: Login, refresh, logout endpoints
- `RefreshTokenService.java`: Refresh token database operations
- `JwtProperties.java`: Configuration properties 
